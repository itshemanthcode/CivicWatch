# Free AI Email Notification Setup

This document explains how to set up the free AI-powered email notification system for sending emails to authorities.

## Overview

The application uses **Google Gemini AI** (free tier) to compose professional emails and **JavaMail** (free) to send emails directly to authorities when issues reach the "NOTIFIED" status. This is completely free - no paid services required!

## Features

- ✅ **100% Free** - Uses Google Gemini AI (free tier) and standard SMTP
- ✅ **AI-Powered** - Gemini AI composes professional, contextual emails
- ✅ **Automatic** - Triggers when issues reach 5 upvotes or status changes to NOTIFIED
- ✅ **Location-Based** - Finds relevant authorities based on issue location
- ✅ **No External Dependencies** - Works directly from your Android app

## Setup Instructions

### Step 1: Get Your Free Gemini API Key

1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy your API key

**Free Tier Limits:**
- 15 requests per minute
- 1,500 requests per day
- More than enough for email notifications!

### Step 2: Configure Email SMTP Settings

You have several free options:

#### Option A: Gmail (Recommended - Easiest)

1. Go to your Google Account settings
2. Enable 2-Step Verification (required for App Passwords)
3. Go to [App Passwords](https://myaccount.google.com/apppasswords)
4. Create a new App Password for "Mail"
5. Copy the 16-character password

**SMTP Settings for Gmail:**
- Host: `smtp.gmail.com`
- Port: `587`
- Username: Your Gmail address
- Password: The App Password (not your regular password)

#### Option B: Outlook/Hotmail (Free)

1. Go to [Microsoft Account Security](https://account.microsoft.com/security)
2. Enable 2-Step Verification
3. Create an App Password for "Mail"

**SMTP Settings for Outlook:**
- Host: `smtp-mail.outlook.com`
- Port: `587`
- Username: Your Outlook email
- Password: App Password

#### Option C: Any SMTP Server

You can use any email provider that supports SMTP:
- Yahoo Mail
- Your organization's email server
- Free SMTP services

### Step 3: Configure the App

1. Open `app/src/main/java/com/example/claudeapp/data/repository/EmailNotificationRepository.kt`

2. Update the following constants:

```kotlin
// Replace with your Gemini API key
private val geminiApiKey = "YOUR_GEMINI_API_KEY"

// Replace with your SMTP settings
private val smtpHost = "smtp.gmail.com"  // or your SMTP server
private val smtpPort = "587"
private val smtpUsername = "your-email@gmail.com"
private val smtpPassword = "your-app-password"  // Gmail App Password
private val fromEmail = "your-email@gmail.com"
```

### Step 4: Test the Integration

1. Create a test issue in the app
2. Upvote it 5 times (or manually change status to NOTIFIED)
3. Check the Android logcat for success/error messages
4. Verify that emails were sent to authorities

## How It Works

1. **Issue Reaches Threshold**: When an issue gets 5 upvotes, status automatically changes to "NOTIFIED"

2. **Authority Lookup**: The app queries Firestore for authorities matching the issue's city and state

3. **AI Email Composition**: Gemini AI composes a professional email with:
   - Issue details (category, severity, description)
   - Location information
   - Community support metrics (upvotes, verifications)
   - Urgency based on severity
   - Professional tone and formatting

4. **Email Delivery**: JavaMail sends the email via SMTP to all relevant authorities

## Email Content

The AI composes emails that include:
- Professional greeting
- Issue category and severity
- Detailed description
- Exact location with address and coordinates
- Community support (upvotes, verifications)
- Priority score
- Request for timely action
- Professional closing

## Free Tier Limits

### Gemini AI
- **15 requests/minute** - More than enough for notifications
- **1,500 requests/day** - Plenty for a civic engagement app
- **Free forever** - No credit card required

### Email (Gmail/Outlook)
- **Gmail**: 500 emails/day (free account)
- **Outlook**: 300 emails/day (free account)
- More than sufficient for authority notifications

## Troubleshooting

### "Failed to generate email content"

**Solution:**
- Check that your Gemini API key is correct
- Verify you haven't exceeded the free tier limits
- Check internet connectivity

### "Failed to send email: Authentication failed"

**Solution:**
- For Gmail: Make sure you're using an **App Password**, not your regular password
- Verify 2-Step Verification is enabled
- Check that SMTP settings are correct

### "No authorities found"

**Solution:**
- Ensure authorities are added to Firestore with correct city/state
- Check that authority documents have `jurisdiction.city` and `jurisdiction.state` fields
- Verify email addresses are present in authority documents

### Emails Not Sending

**Check:**
1. SMTP host and port are correct
2. App Password is valid (for Gmail/Outlook)
3. Firewall/network isn't blocking SMTP port 587
4. Check Android logcat for detailed error messages

## Security Best Practices

1. **API Key Security**: Consider storing the Gemini API key in a secure configuration file or environment variable
2. **App Passwords**: Never commit App Passwords to version control
3. **Email Validation**: The app validates authority email addresses before sending
4. **Error Handling**: Failures don't expose sensitive information

## Advanced Configuration

### Custom Email Templates

You can modify the prompt in `composeEmailWithGemini()` to customize:
- Email tone (formal, urgent, friendly)
- Additional information to include
- Formatting preferences
- Language (if needed)

### Multiple Email Accounts

You can create multiple `EmailNotificationRepository` instances with different SMTP settings for:
- Different regions
- Different issue categories
- Backup email accounts

## Cost Breakdown

- **Gemini AI**: $0 (Free tier)
- **SMTP Email**: $0 (Gmail/Outlook free accounts)
- **Total Cost**: **$0/month** 🎉

## Alternative Free Options

If you prefer other free AI services:

1. **Hugging Face Inference API** - Free tier available
2. **Cohere API** - Free tier with 100 requests/month
3. **OpenAI API** - $5 free credits (one-time)

But Gemini is recommended as it's already integrated and has the most generous free tier!

## Support

For issues or questions:
1. Check Android logcat for detailed error messages
2. Verify all configuration values are correct
3. Test SMTP settings with a simple email client first
4. Ensure authorities are properly configured in Firestore

---

**Note**: This solution is completely free and doesn't require any paid services like Zapier. All functionality runs directly in your Android app!


