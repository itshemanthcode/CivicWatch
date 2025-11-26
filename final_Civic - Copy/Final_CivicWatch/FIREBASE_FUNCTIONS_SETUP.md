# Firebase Cloud Functions Setup - Civic Issue Notification Escalator

This guide explains how to set up Firebase Cloud Functions to automatically monitor and escalate civic issues using Google Gemini AI.

## Overview

The **Civic Issue Notification Escalator** runs as a Firebase Cloud Function that:
- ✅ **Monitors Firebase 24/7** - Automatically watches for issues with 5+ upvotes
- ✅ **Retrieves Complete Details** - Gets all issue information (photos, GPS, timestamps, etc.)
- ✅ **Determines Authority** - Finds the right department based on issue type and location
- ✅ **Sends Structured Email** - Uses Gemini AI to compose professional emails
- ✅ **Updates Status** - Changes status from "Verified" to "Notified" with timestamp

## Prerequisites

1. **Firebase CLI** installed:
   ```bash
   npm install -g firebase-tools
   ```

2. **Node.js 18+** installed

3. **Firebase Project** with:
   - Firestore Database enabled
   - Billing enabled (Blaze plan required for Cloud Functions)
   - Functions API enabled

## Setup Instructions

### Step 1: Install Dependencies

```bash
cd functions
npm install
```

### Step 2: Configure Firebase

1. Login to Firebase:
   ```bash
   firebase login
   ```

2. Initialize Firebase Functions (if not already done):
   ```bash
   firebase init functions
   ```
   - Select your Firebase project
   - Choose JavaScript
   - Install dependencies: Yes

### Step 3: Set Configuration

Set your API keys and email credentials:

```bash
# Set Gemini API Key
firebase functions:config:set gemini.api_key="YOUR_GEMINI_API_KEY"

# Set SMTP settings (Gmail example)
firebase functions:config:set smtp.host="smtp.gmail.com"
firebase functions:config:set smtp.port="587"
firebase functions:config:set smtp.user="your-email@gmail.com"
firebase functions:config:set smtp.pass="your-app-password"
firebase functions:config:set smtp.from="your-email@gmail.com"
```

**For Gmail App Password:**
1. Go to [Google Account Security](https://myaccount.google.com/security)
2. Enable 2-Step Verification
3. Go to [App Passwords](https://myaccount.google.com/apppasswords)
4. Create an App Password for "Mail"
5. Use the 16-character password (not your regular Gmail password)

### Step 4: Deploy Functions

```bash
firebase deploy --only functions
```

### Step 5: Verify Deployment

Check Firebase Console → Functions to see your deployed function:
- `monitorCivicIssues` - Triggers on issue updates

## How It Works

### 1. **Monitors Firebase**

The function automatically triggers when any issue document in Firestore is updated:

```javascript
exports.monitorCivicIssues = functions.firestore
    .document('issues/{issueId}')
    .onUpdate(async (change, context) => {
      // Checks if upvotes reached 5+
      // Updates status to "notified"
      // Triggers email escalation
    });
```

### 2. **Retrieves Issue Details**

When triggered, the function retrieves:
- Issue ID, category, severity, description
- Location (address, city, state, GPS coordinates)
- Images/photos URLs
- Timestamps (created, notified)
- Validation counts (upvotes, verifications)
- Priority score

### 3. **Determines Authority**

Three-tier matching system:

1. **Category-Based**: Finds authorities with `handledCategories` containing the issue category
2. **Department-Based**: Maps category to department type (road_department, utilities, etc.)
3. **Location-Based**: Falls back to all authorities for the location (city + state)

### 4. **Sends Structured Email**

Uses **Google Gemini AI** to compose a professional email with:
- Issue summary
- Location details with Google Maps link
- Community validation metrics
- Media availability
- Professional request for action

### 5. **Updates Status**

Automatically:
- Changes status from "verified"/"reported" to "notified"
- Sets `notifiedAt` timestamp
- Updates `updatedAt` timestamp

## Authority Data Structure

Ensure authorities in Firestore have this structure:

```javascript
{
  organizationName: "City Road Department",
  email: "roads@city.gov",
  jurisdiction: {
    city: "Springfield",
    state: "IL"
  },
  handledCategories: ["potholes", "road_damage"],
  departmentType: "road_department",
  notificationPreferences: {
    email: true,
    threshold: 5
  }
}
```

## Testing

### Test Locally (Emulator)

```bash
# Start emulator
firebase emulators:start --only functions,firestore

# In another terminal, test the function
# Update an issue document in Firestore to trigger the function
```

### Test in Production

1. Create a test issue in your app
2. Upvote it 5 times
3. Check Firebase Console → Functions → Logs
4. Verify email was sent
5. Check issue status changed to "notified"

## Monitoring

### View Logs

```bash
firebase functions:log
```

Or in Firebase Console → Functions → Logs

### Common Log Messages

- `🚨 Issue [ID] reached [X] upvotes - triggering escalation`
- `✅ Updated issue [ID] status to "notified"`
- `📧 Found [X] relevant authority/authorities`
- `✅ Successfully escalated issue [ID] to authorities`
- `❌ Error escalating issue [ID]: [error]`

## Cost

### Free Tier (Spark Plan)
- ❌ Cloud Functions not available on Spark plan
- Requires Blaze (pay-as-you-go) plan

### Blaze Plan Costs
- **Cloud Functions**: 
  - First 2 million invocations/month: FREE
  - $0.40 per million invocations after
- **Gemini AI**: FREE (15 requests/min, 1,500/day)
- **Email (Gmail)**: FREE (500 emails/day)
- **Firestore**: FREE tier includes 50K reads/day

**Estimated Cost**: ~$0-5/month for typical usage

## Troubleshooting

### Function Not Triggering

**Check:**
1. Function is deployed: `firebase functions:list`
2. Firestore rules allow writes
3. Issue document is actually being updated
4. Upvotes are reaching 5+

### Email Not Sending

**Check:**
1. SMTP credentials are correct
2. Gmail App Password is valid (not regular password)
3. Check function logs for SMTP errors
4. Verify authority email addresses are valid

### No Authorities Found

**Check:**
1. Authorities exist in Firestore `authorities` collection
2. `jurisdiction.city` and `jurisdiction.state` match issue location
3. `notificationPreferences.email` is `true`
4. `handledCategories` or `departmentType` are set correctly

### Gemini AI Errors

**Check:**
1. API key is correct in function config
2. Not exceeding free tier limits (15/min, 1,500/day)
3. Check function logs for Gemini API errors

## Security

### Environment Variables

Never commit sensitive data. Use Firebase Functions config:

```bash
firebase functions:config:set secret.key="value"
```

### Firestore Rules

Ensure proper security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /issues/{issueId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    match /authorities/{authorityId} {
      allow read: if true;
      allow write: if request.auth != null && 
                     get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
  }
}
```

## Advanced Configuration

### Custom Email Templates

Modify the prompt in `composeEmailWithGemini()` function to customize email format.

### Retry Logic

Add retry logic for failed email sends:

```javascript
const maxRetries = 3;
for (let i = 0; i < maxRetries; i++) {
  try {
    await sendEmailToAuthorities(issue, authorities);
    break;
  } catch (error) {
    if (i === maxRetries - 1) throw error;
    await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
  }
}
```

### Scheduled Monitoring

Add a scheduled function to check for missed escalations:

```javascript
exports.scheduledEscalationCheck = functions.pubsub
    .schedule('every 1 hours')
    .onRun(async (context) => {
      // Check for issues with 5+ upvotes that aren't notified
    });
```

## Comparison: Cloud Functions vs App-Based

| Feature | Cloud Functions | App-Based |
|---------|----------------|-----------|
| **24/7 Monitoring** | ✅ Yes | ❌ Only when app is open |
| **Reliability** | ✅ High | ⚠️ Depends on app usage |
| **Cost** | 💰 Blaze plan required | ✅ Free |
| **Setup Complexity** | ⚠️ Medium | ✅ Easy |
| **Scalability** | ✅ Excellent | ⚠️ Limited |

## Next Steps

1. ✅ Deploy functions to Firebase
2. ✅ Configure API keys and SMTP
3. ✅ Add authorities to Firestore
4. ✅ Test with a sample issue
5. ✅ Monitor logs for any issues

---

**Note**: This solution provides 24/7 monitoring and is more reliable than app-based monitoring, but requires a Firebase Blaze plan.


