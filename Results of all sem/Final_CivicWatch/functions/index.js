/**
 * Civic Issue Notification Escalator
 * Firebase Cloud Functions for monitoring and escalating civic issues
 * 
 * This function monitors Firebase Firestore for civic issues that have
 * received 5+ upvotes and automatically notifies appropriate authorities.
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');
const {GoogleGenerativeAI} = require('@google/generative-ai');
const nodemailer = require('nodemailer');

admin.initializeApp();

// Configuration - Set these in Firebase Functions config
const GEMINI_API_KEY = functions.config().gemini?.api_key || process.env.GEMINI_API_KEY;
const SMTP_HOST = functions.config().smtp?.host || 'smtp.gmail.com';
const SMTP_PORT = functions.config().smtp?.port || 587;
const SMTP_USER = functions.config().smtp?.user || '';
const SMTP_PASS = functions.config().smtp?.pass || '';
const FROM_EMAIL = functions.config().smtp?.from || '';

// Initialize Gemini AI
const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);

// Email transporter
const transporter = nodemailer.createTransport({
  host: SMTP_HOST,
  port: SMTP_PORT,
  secure: false,
  auth: {
    user: SMTP_USER,
    pass: SMTP_PASS,
  },
});

/**
 * Monitor Firestore for issues that reach 5+ upvotes
 * This triggers automatically when an issue document is updated
 */
exports.monitorCivicIssues = functions.firestore
    .document('issues/{issueId}')
    .onUpdate(async (change, context) => {
      const before = change.before.data();
      const after = change.after.data();
      const issueId = context.params.issueId;

      // Check if upvotes reached 5 and status should change to "notified"
      const upvotesBefore = before.upvotes || 0;
      const upvotesAfter = after.upvotes || 0;
      const currentStatus = (after.status || '').toLowerCase();
      const wasNotified = currentStatus === 'notified';
      const isResolved = currentStatus === 'resolved';
      const isRejected = currentStatus === 'rejected';

      // Only process if:
      // 1. Upvotes reached 5 or more
      // 2. Status is not already "notified", "resolved", or "rejected"
      // 3. Upvotes increased (to avoid duplicate processing)
      if (upvotesAfter >= 5 && 
          !wasNotified && 
          !isResolved && 
          !isRejected && 
          upvotesAfter > upvotesBefore) {
        
        console.log(`🚨 Issue ${issueId} reached ${upvotesAfter} upvotes - triggering escalation`);

        try {
          // Step 1: Update status to "notified"
          await change.after.ref.update({
            status: 'notified',
            notifiedAt: admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          console.log(`✅ Updated issue ${issueId} status to "notified"`);

          // Step 2: Retrieve complete issue details
          const issueData = {
            issueId: issueId,
            category: after.category || '',
            severity: after.severity || 'medium',
            description: after.description || '',
            status: 'notified',
            upvotes: upvotesAfter,
            verifications: after.verifications || 0,
            reportedBy: after.reportedBy || '',
            createdAt: after.createdAt,
            notifiedAt: admin.firestore.Timestamp.now(),
            location: after.location || {},
            images: after.images || [],
            priorityScore: after.priorityScore || 0,
            commentsCount: after.commentsCount || 0,
          };

          // Step 3: Determine relevant authorities
          const authorities = await determineRelevantAuthorities(issueData);

          if (authorities.length === 0) {
            console.warn(`⚠️ No authorities found for issue ${issueId}`);
            return null;
          }

          console.log(`📧 Found ${authorities.length} relevant authority/authorities for issue ${issueId}`);

          // Step 4: Compose and send email using Gemini AI
          await sendEmailToAuthorities(issueData, authorities);

          console.log(`✅ Successfully escalated issue ${issueId} to authorities`);

          return null;
        } catch (error) {
          console.error(`❌ Error escalating issue ${issueId}:`, error);
          // Don't throw - log error but don't fail the function
          return null;
        }
      }

      return null;
    });

/**
 * Determine relevant authorities based on issue category and location
 */
async function determineRelevantAuthorities(issue) {
  try {
    const city = issue.location?.city || '';
    const state = issue.location?.state || '';
    const category = (issue.category || '').toLowerCase();

    if (!city || !state) {
      console.warn('Missing location information for authority lookup');
      return [];
    }

    // First, try to find authorities that handle this specific category
    let authorities = [];
    
    try {
      const categoryQuery = await admin.firestore()
          .collection('authorities')
          .where('jurisdiction.city', '==', city)
          .where('jurisdiction.state', '==', state)
          .where('handledCategories', 'array-contains', category)
          .where('notificationPreferences.email', '==', true)
          .get();

      authorities = categoryQuery.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));

      if (authorities.length > 0) {
        console.log(`Found ${authorities.length} category-based authorities`);
        return authorities;
      }
    } catch (error) {
      console.warn('Error fetching category-based authorities:', error);
    }

    // Fallback: Find by department type
    const departmentType = mapCategoryToDepartmentType(category);
    
    try {
      const deptQuery = await admin.firestore()
          .collection('authorities')
          .where('jurisdiction.city', '==', city)
          .where('jurisdiction.state', '==', state)
          .where('departmentType', '==', departmentType)
          .where('notificationPreferences.email', '==', true)
          .get();

      authorities = deptQuery.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));

      if (authorities.length > 0) {
        console.log(`Found ${authorities.length} department-based authorities`);
        return authorities;
      }
    } catch (error) {
      console.warn('Error fetching department-based authorities:', error);
    }

    // Final fallback: Get all authorities for location
    try {
      const locationQuery = await admin.firestore()
          .collection('authorities')
          .where('jurisdiction.city', '==', city)
          .where('jurisdiction.state', '==', state)
          .where('notificationPreferences.email', '==', true)
          .get();

      authorities = locationQuery.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));

      console.log(`Found ${authorities.length} location-based authorities (fallback)`);
      return authorities;
    } catch (error) {
      console.error('Error fetching location-based authorities:', error);
      return [];
    }
  } catch (error) {
    console.error('Error determining authorities:', error);
    return [];
  }
}

/**
 * Map issue category to department type
 */
function mapCategoryToDepartmentType(category) {
  const categoryLower = category.toLowerCase();
  
  if (categoryLower.includes('pothole') || 
      categoryLower.includes('road') || 
      categoryLower.includes('street')) {
    return 'road_department';
  }
  
  if (categoryLower.includes('light')) {
    return 'utilities';
  }
  
  if (categoryLower.includes('garbage') || 
      categoryLower.includes('trash') || 
      categoryLower.includes('waste')) {
    return 'sanitation';
  }
  
  if (categoryLower.includes('water')) {
    return 'public_works';
  }
  
  return 'public_works'; // Default
}

/**
 * Compose email using Gemini AI and send to authorities
 */
async function sendEmailToAuthorities(issue, authorities) {
  try {
    // Compose email using Gemini AI
    const emailContent = await composeEmailWithGemini(issue, authorities);

    // Get all authority email addresses
    const authorityEmails = authorities
        .map((auth) => auth.email)
        .filter((email) => email && email.trim() !== '');

    if (authorityEmails.length === 0) {
      throw new Error('No valid email addresses found for authorities');
    }

    // Format subject line
    const categoryDisplay = issue.category
        .replace(/_/g, ' ')
        .split(' ')
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
    
    const subject = `Civic Issue Report: ${categoryDisplay} - ${issue.location.city}, ${issue.location.state}`;

    // Send email to all authorities
    const mailOptions = {
      from: FROM_EMAIL,
      to: authorityEmails.join(', '),
      subject: subject,
      text: emailContent,
    };

    await transporter.sendMail(mailOptions);
    console.log(`📧 Email sent to ${authorityEmails.length} authority/authorities`);

    return true;
  } catch (error) {
    console.error('Error sending email:', error);
    throw error;
  }
}

/**
 * Use Gemini AI to compose professional email
 */
async function composeEmailWithGemini(issue, authorities) {
  try {
    const model = genAI.getGenerativeModel({model: 'gemini-2.0-flash-001'});

    const prompt = `
You are the "Civic Issue Notification Escalator" - an AI system that automatically escalates community-reported civic issues to local authorities.

Your task: Compose a comprehensive, structured email report to notify authorities about a civic issue that has received sufficient community validation.

=== ISSUE DETAILS ===
Issue ID: ${issue.issueId}
Category: ${issue.category}
Severity: ${issue.severity}
Description: ${issue.description || 'No description provided'}

=== LOCATION INFORMATION ===
Address: ${issue.location?.address || 'Not specified'}
Area/Neighborhood: ${issue.location?.area || 'Not specified'}
City: ${issue.location?.city || 'Not specified'}
State: ${issue.location?.state || 'Not specified'}
Country: ${issue.location?.country || 'Not specified'}
GPS Coordinates: ${issue.location?.latitude || 0}, ${issue.location?.longitude || 0}
Google Maps Link: https://www.google.com/maps?q=${issue.location?.latitude || 0},${issue.location?.longitude || 0}

=== COMMUNITY VALIDATION ===
Upvotes: ${issue.upvotes} (indicates community support and priority)
Verifications: ${issue.verifications || 0} (photo/community verifications)
Priority Score: ${issue.priorityScore || 0}
Status: ${issue.status}

=== TIMELINE ===
Reported on: ${issue.createdAt ? issue.createdAt.toDate().toLocaleString() : 'Unknown'}
Notified on: ${issue.notifiedAt ? issue.notifiedAt.toDate().toLocaleString() : 'Just now'}

=== MEDIA ===
${issue.images && issue.images.length > 0 
  ? `Images: ${issue.images.join(', ')}` 
  : 'No images available'}

=== RECIPIENTS ===
${authorities.map((auth) => 
  `- ${auth.organizationName || 'Authority'} (${auth.email})${auth.departmentType ? ` - ${auth.departmentType}` : ''}`
).join('\n')}

=== EMAIL REQUIREMENTS ===
1. Write a professional, formal email suitable for government authorities
2. Structure the email with clear sections:
   - Greeting and introduction
   - Issue summary (category, severity, description)
   - Detailed location information (include Google Maps link)
   - Community validation metrics (upvotes, verifications)
   - Media availability (mention images if available)
   - Request for action
   - Contact information
3. Emphasize urgency based on severity level (${issue.severity})
4. Highlight community support (${issue.upvotes} upvotes shows this is a priority for residents)
5. Include all technical details (GPS coordinates, timestamps)
6. Be respectful, professional, and concise
7. Request acknowledgment or status update if possible

Format the email as plain text (no HTML, no markdown). Start directly with the greeting (e.g., "Dear [Authority Name]," or "Dear Sir/Madam,").
    `.trim();

    const result = await model.generateContent(prompt);
    const response = await result.response;
    const emailContent = response.text();

    if (!emailContent) {
      throw new Error('Failed to generate email content from Gemini');
    }

    return emailContent;
  } catch (error) {
    console.error('Error composing email with Gemini:', error);
    
    // Fallback email template
    return `
Dear Authorities,

We are writing to report a civic issue that requires your attention:

Category: ${issue.category}
Severity: ${issue.severity}
Description: ${issue.description || 'No description provided'}

Location:
${issue.location?.address || 'Address not specified'}
${issue.location?.area ? `${issue.location.area}, ` : ''}${issue.location?.city || ''}, ${issue.location?.state || ''}
GPS Coordinates: ${issue.location?.latitude || 0}, ${issue.location?.longitude || 0}
Google Maps: https://www.google.com/maps?q=${issue.location?.latitude || 0},${issue.location?.longitude || 0}

Community Support:
- ${issue.upvotes} upvotes
- ${issue.verifications || 0} verifications
- Priority Score: ${issue.priorityScore || 0}

Reported on: ${issue.createdAt ? issue.createdAt.toDate().toLocaleString() : 'Unknown'}

${issue.images && issue.images.length > 0 ? `Images are available: ${issue.images.join(', ')}\n` : ''}

We request your timely attention to resolve this issue for the benefit of our community.

Thank you for your service.

Sincerely,
CivicWatch Platform
    `.trim();
  }
}


