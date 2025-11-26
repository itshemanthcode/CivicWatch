# Civic Issue Notification Escalator

## Overview

The **Civic Issue Notification Escalator** is an AI-powered system that automatically monitors Firebase for civic issues that have received sufficient community validation (5+ upvotes) and escalates them to the appropriate local authorities via professional email notifications.

## How It Works

### 1. **Monitors Firebase**
- Continuously watches for civic issue documents in Firestore
- Detects when issues reach the validation threshold (5 upvotes)
- Tracks issue status changes

### 2. **Retrieves Issue Details**
When an issue reaches 5+ upvotes, the system automatically:
- Retrieves complete issue information including:
  - Photos/images
  - GPS coordinates
  - Timestamps (reported, verified, notified)
  - Descriptions
  - Validation counts (upvotes, verifications)
  - Priority scores
  - Location details (address, city, state, area)

### 3. **Determines Authority**
The system intelligently determines which local department to contact based on:

**Primary Method - Category Matching:**
- Searches for authorities that have the issue category in their `handledCategories` array
- Example: Potholes → Road Department, Broken Street Lights → Utilities Department

**Secondary Method - Department Type:**
- Maps issue category to department type:
  - **Potholes/Roads** → `road_department`
  - **Street Lights** → `utilities`
  - **Garbage/Waste** → `sanitation`
  - **Water Logging** → `public_works`

**Fallback Method - Location-Based:**
- If no category-specific authority found, uses all authorities for the location (city + state)

### 4. **Sends Structured Email**
Uses **Google Gemini AI** (free tier) to compose a comprehensive, professional email report that includes:

- **Issue Summary**: Category, severity, description
- **Location Details**: Full address, GPS coordinates, Google Maps link
- **Community Validation**: Upvotes count, verifications count, priority score
- **Media**: Image URLs (if available)
- **Timeline**: When reported, when notified
- **Action Request**: Professional request for timely resolution

### 5. **Updates Status**
- Changes issue status from "Verified" (or "Reported") to **"Notified"**
- Adds `notifiedAt` timestamp
- Updates `updatedAt` timestamp
- Logs escalation success/failure

## Authority Data Model

To use the escalator effectively, authorities should be stored in Firestore with:

```javascript
{
  authorityId: "auth_123",
  organizationName: "City Road Department",
  email: "roads@city.gov",
  jurisdiction: {
    city: "Springfield",
    state: "IL",
    areas: ["Downtown", "North Side"]
  },
  handledCategories: ["potholes", "road_damage"], // Categories this authority handles
  departmentType: "road_department", // Type of department
  notificationPreferences: {
    email: true,
    threshold: 5 // Minimum upvotes to trigger notification
  }
}
```

## Issue Categories → Department Mapping

| Issue Category | Department Type | Example Authority |
|---------------|----------------|-------------------|
| Potholes, Road Damage | `road_department` | City Road Department |
| Broken Street Lights | `utilities` | City Utilities Department |
| Garbage, Waste, Trash | `sanitation` | Sanitation Department |
| Water Logging, Flooding | `public_works` | Public Works Department |
| Other | `public_works` | General Public Works |

## Workflow Example

1. **User reports pothole** → Status: "reported"
2. **Community upvotes reach 5** → Status automatically changes to "notified"
3. **System determines authority**:
   - Searches for authorities with `handledCategories` containing "potholes"
   - Or searches for `departmentType = "road_department"`
   - Matches by location (city + state)
4. **AI composes email** with all issue details
5. **Email sent** to Road Department
6. **Status updated** with `notifiedAt` timestamp

## Configuration

### Authority Setup in Firestore

Create authority documents in the `authorities` collection:

```javascript
// Example: Road Department
{
  organizationName: "City Road Department",
  email: "roads@city.gov",
  jurisdiction: {
    city: "YourCity",
    state: "YourState"
  },
  handledCategories: ["potholes", "road_damage", "street_repair"],
  departmentType: "road_department",
  notificationPreferences: {
    email: true,
    threshold: 5
  }
}

// Example: Utilities Department
{
  organizationName: "City Utilities",
  email: "utilities@city.gov",
  jurisdiction: {
    city: "YourCity",
    state: "YourState"
  },
  handledCategories: ["broken_street_lights", "street_lighting"],
  departmentType: "utilities",
  notificationPreferences: {
    email: true,
    threshold: 5
  }
}
```

## Email Format

The AI-generated email includes:

```
Subject: Civic Issue Report: Potholes - Springfield, IL

Dear City Road Department,

[Professional introduction]

Issue Summary:
- Category: Potholes
- Severity: High
- Description: [User's description]

Location:
- Address: [Full address]
- GPS: [Latitude], [Longitude]
- Google Maps: [Link]

Community Validation:
- 5 upvotes (indicates strong community support)
- 2 verifications
- Priority Score: 8.5

Media: [Image URLs if available]

Timeline:
- Reported: [Date/Time]
- Notified: [Date/Time]

[Request for action and closing]
```

## Monitoring & Logging

The escalator logs all activities:

- ✅ Success: `Successfully escalated issue [ID] to authorities via email`
- ❌ Failure: `Failed to escalate issue [ID] to authorities: [error]`
- ℹ️ Info: `Found X relevant authority/authorities for issue [ID]`

Check Android logcat for detailed logs.

## Free Tier Limits

- **Gemini AI**: 15 requests/minute, 1,500/day (free forever)
- **Email (Gmail)**: 500 emails/day (free account)
- **Total Cost**: $0/month

## Troubleshooting

### No Authorities Found

**Problem**: `No relevant authorities found for issue`

**Solutions**:
1. Ensure authorities are added to Firestore `authorities` collection
2. Verify `jurisdiction.city` and `jurisdiction.state` match issue location
3. Check that `handledCategories` or `departmentType` are set correctly
4. Verify `notificationPreferences.email` is `true`

### Email Not Sending

**Problem**: `Failed to escalate issue to authorities`

**Solutions**:
1. Check SMTP settings in `EmailNotificationRepository.kt`
2. Verify Gmail App Password is correct (not regular password)
3. Check internet connectivity
4. Review Android logcat for specific error messages

### Status Not Changing

**Problem**: Issue doesn't change to "Notified" at 5 upvotes

**Solutions**:
1. Verify upvote count is actually 5+
2. Check that issue status is not already "resolved" or "rejected"
3. Review transaction logs in Firestore
4. Check Android logcat for transaction errors

## Future Enhancements

- [ ] Firebase Cloud Function for server-side monitoring (24/7)
- [ ] Retry logic for failed email sends
- [ ] Email templates per authority type
- [ ] SMS notifications for urgent issues
- [ ] Dashboard for authority response tracking
- [ ] Multi-language support for emails

---

**Note**: This system is completely free and runs directly in your Android app using Google Gemini AI and standard SMTP email.


