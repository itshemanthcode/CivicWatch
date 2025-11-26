# Zapier AI Agent Integration Setup

This document explains how to set up and configure the Zapier AI agent integration for the Civic Issue Notification Escalator.

## Overview

The application integrates with a **Zapier AI agent** called "Civic Issue Notification Escalator" to automatically send emails to authorities when issues reach the "NOTIFIED" status. This happens automatically when:
1. An issue receives 5 or more upvotes (auto-changes status to NOTIFIED)
2. An admin manually changes an issue status to NOTIFIED

## How It Works

### 1. **Monitors Firebase**
- The app watches for civic issue documents where the upvote/validation count reaches 5 or more
- Automatically triggers when threshold is reached

### 2. **Retrieves Issue Details**
- Gets complete information including:
  - Photos/images
  - GPS coordinates
  - Timestamps (reported, verified, notified)
  - Descriptions
  - Validation counts (upvotes, verifications)
  - Priority scores
  - Location details (address, city, state, area)

### 3. **Determines Authority**
- Figures out which local department to contact based on:
  - Issue type (road dept for potholes, utilities for streetlights, etc.)
  - Location (city and state)
- Uses three-tier matching:
  1. Category-based (handledCategories)
  2. Department-based (departmentType)
  3. Location-based (city + state)

### 4. **Sends Structured Email**
- Creates a comprehensive report with all issue details
- Sends it to the relevant authority via Zapier AI agent
- The AI agent composes professional emails automatically

### 5. **Updates Status**
- Changes the issue status from "Verified" (or "Reported") to "Notified"
- Adds a `notifiedAt` timestamp
- Updates `updatedAt` timestamp

## Setup Instructions

### Step 1: Get Your Zapier Webhook URL

1. Go to your Zapier AI agent: https://agents.zapier.com/copy/21835688-3b77-4412-b95e-f309fa5e2bbb
2. Create a new Zap or use an existing one
3. Add a "Webhooks by Zapier" trigger
4. Choose "Catch Hook" as the trigger
5. Copy the webhook URL (it will look like: `https://hooks.zapier.com/hooks/catch/XXXXX/YYYYY/`)

### Step 2: Configure the Webhook URL in the App

1. Open `app/src/main/java/com/example/claudeapp/data/repository/ZapierRepository.kt`
2. Find the line: `private val zapierWebhookUrl = "https://hooks.zapier.com/hooks/catch/YOUR_WEBHOOK_ID/"`
3. Replace `YOUR_WEBHOOK_ID` with your actual webhook URL from Step 1

### Step 3: Configure Your Zapier Workflow

Your Zapier workflow should:

1. **Trigger**: Webhooks by Zapier - Catch Hook
   - This receives the issue data from the Android app

2. **Action**: Zapier AI Agent (Civic Issue Notification Escalator)
   - Configure the AI agent to:
     - Read the issue details from the webhook payload
     - Identify relevant authorities based on issue type and location
     - Compose a professional email to the authorities
     - Include issue details: category, severity, description, location, images

3. **Action**: Email (Gmail, Outlook, etc.)
   - Send the composed email to the authority email addresses
   - Use the email addresses from the `authorities` array in the payload

### Step 4: Test the Integration

1. Create a test issue in the app
2. Upvote it 5 times (or manually change status to NOTIFIED)
3. Check your Zapier dashboard to see if the webhook was triggered
4. Verify that emails were sent to the authorities

## Webhook Payload Structure

The app sends the following JSON structure to Zapier:

```json
{
  "issueId": "string",
  "category": "string",
  "severity": "string",
  "description": "string",
  "status": "notified",
  "upvotes": 5,
  "verifications": 0,
  "reportedBy": "userId",
  "createdAt": "timestamp",
  "location": {
    "address": "string",
    "area": "string",
    "city": "string",
    "state": "string",
    "country": "string",
    "latitude": 0.0,
    "longitude": 0.0
  },
  "images": ["url1", "url2"],
  "authorities": [
    {
      "organizationName": "string",
      "email": "string",
      "city": "string",
      "state": "string",
      "departmentType": "string",
      "handledCategories": ["potholes", "road_damage"]
    }
  ],
  "priorityScore": 0.0,
  "commentsCount": 0,
  "notifiedAt": "timestamp"
}
```

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

## Features

- **Automatic Notification**: Issues automatically change to NOTIFIED status when they reach 5 upvotes
- **Location-Based Authorities**: The app fetches relevant authorities based on the issue's city and state
- **AI-Powered Emails**: Zapier AI agent composes professional emails to authorities
- **Error Handling**: Failures in sending notifications don't affect the main app functionality
- **Background Processing**: Notifications are sent asynchronously to avoid blocking the UI

## Troubleshooting

### Webhook Not Receiving Data

1. Check that the webhook URL is correct in `ZapierRepository.kt`
2. Verify your Zapier Zap is active
3. Check Zapier's webhook history to see if requests are being received
4. Check Android logcat for error messages

### Emails Not Being Sent

1. Verify your Zapier workflow is correctly configured
2. Check that the AI agent has proper instructions
3. Verify email account permissions in Zapier
4. Check Zapier task history for errors

### Status Not Changing to NOTIFIED

1. Verify that upvotes are being counted correctly
2. Check that the issue status is not already RESOLVED or REJECTED
3. Check Android logcat for transaction errors

## Security Considerations

- The webhook URL should be kept secure
- Consider using environment variables or a secure configuration file for the webhook URL
- Ensure your Zapier account has proper security settings
- Authority email addresses should be validated before sending

## Cost

- **Zapier**: Free tier includes 100 tasks/month, then $19.99/month for 750 tasks
- **Total Cost**: Free for low volume, or ~$20/month for higher usage

## Future Enhancements

- Add retry logic for failed webhook calls
- Add notification preferences per authority
- Support for multiple email templates based on issue category
- Add webhook signature verification for security

---

**Note**: This solution uses Zapier's AI agent to handle email composition and sending, making it easy to set up and maintain without managing email servers or AI APIs directly.


