# Flossk TTS - User Manual

Welcome to the Flossk TTS User Manual. This guide will help you use the Text-to-Speech API service effectively.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Admin Dashboard](#admin-dashboard)
3. [Managing API Keys](#managing-api-keys)
4. [Using the API](#using-the-api)
5. [Iframe Embedding](#iframe-embedding)
6. [Viewing Usage History](#viewing-usage-history)
7. [Token System Explained](#token-system-explained)
8. [Frequently Asked Questions](#frequently-asked-questions)

---

## Getting Started

### First Login

1. Navigate to the admin dashboard URL (provided by your administrator)
2. Enter your login credentials:
   - **Username**: `floosk`
   - **Password**: `flosskaadmin` (default - change immediately!)
3. Click **Login**

### Change Your Password

**Important**: Change the default password immediately after first login for security.

1. Click **Change Password** in the top navigation
2. Enter your current password
3. Enter your new password (minimum 6 characters)
4. Confirm your new password
5. Click **Change Password**

You'll see a success message when the password is changed.

---

## Admin Dashboard

The admin dashboard is your central control panel for managing API keys, viewing usage statistics, and generating embed codes.

### Dashboard Overview

The dashboard displays:
- **Create New API Key** form - Create new API keys with custom settings
- **Existing API Keys** table - View and manage all your API keys
- Quick access to **History** and **Iframe** pages for each API key

### Navigation

- **Change Password** - Update your admin password
- **Logout** - Sign out of the admin dashboard
- **History** - View detailed usage history for an API key
- **Iframe** - Generate embed codes for an API key

---

## Managing API Keys

### Creating a New API Key

1. Scroll to the **Create New API Key** section
2. Fill in the form:
   - **Owner Name**: Enter a descriptive name (e.g., "My Website", "Mobile App")
   - **Token Limit Type**: Choose from:
     - **Unlimited** - No restrictions
     - **Total** - One-time total limit
     - **Monthly** - Resets each month
     - **Yearly** - Resets each year
     - **Once** - Single use only
   - **Token Limit Value**: Enter the number of tokens (required for all types except Unlimited)
   - **Referer Domain**: Enter the domain where iframes will be embedded (e.g., `example.com`) - leave empty to allow any domain
3. Click **Create API Key**

The new API key will appear in the table below. **Important**: Copy and save your API key immediately - it's shown only once!

### Viewing API Keys

The API keys table shows:
- **ID** - Unique identifier
- **API Key** - Your API key (masked for security - click "Show" to reveal)
- **Owner Name** - The name you assigned
- **Token Limit** - Current limit type and remaining tokens
- **Referer Domain** - Domain restriction for iframes
- **Actions** - Available operations

### Revealing API Keys

API keys are hidden by default for security. To view an API key:

1. Find the API key in the table
2. Click the **Show** button next to the masked key
3. The full API key will be displayed
4. Click **Hide** to mask it again

**Security Tip**: Only reveal API keys when you need to copy them. Never share API keys publicly.

### Editing API Keys

You can update token limits and referer domains after creation:

1. Find the API key in the table
2. Click the **Edit** button
3. Update the following fields:
   - **Token Limit Type** - Change the limit type
   - **Token Limit Value** - Update the token amount
   - **Referer Domain** - Change or remove domain restriction
4. Click **Update**
5. Click **Cancel** to close without saving

**Note**: Changing token limit types may reset usage counters. Monthly and Yearly limits reset automatically at the start of each period.

### Deleting API Keys

1. Find the API key in the table
2. Click the **Delete** button
3. Confirm the deletion in the popup

**Warning**: Deleting an API key is permanent and cannot be undone. All associated usage history will be removed.

---

## Using the API

### API Endpoint

**URL**: `POST /api/tts/speak`

**Base URL**: Your server URL (e.g., `https://api.example.com`)

### Authentication

Include your API key in the request header:

```
X-API-KEY: your-api-key-here
```

### Request Format

Send a JSON request with the following fields:

```json
{
  "text": "Your text to convert to speech",
  "voiceId": "edon"
}
```

**Fields**:
- **text** (required): The text you want to convert to speech
- **voiceId** (required): The voice to use (`edon`, `arta`, `arben`, or `dren`)

### Available Voices

- **edon** - Default voice
- **arta** - Alternative voice
- **arben** - Alternative voice
- **dren** - Alternative voice

### Response

The API returns a WAV audio file that you can:
- Save to disk
- Play directly
- Stream to users
- Embed in web pages

### Example: Using cURL

```bash
curl -X POST https://your-server.com/api/tts/speak \
  -H "X-API-KEY: your-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{"text":"Hello, this is a test message.","voiceId":"edon"}' \
  --output speech.wav
```

### Example: Using JavaScript

```javascript
const response = await fetch('https://your-server.com/api/tts/speak', {
  method: 'POST',
  headers: {
    'X-API-KEY': 'your-api-key-here',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    text: 'Hello, this is a test message.',
    voiceId: 'edon'
  })
});

const audioBlob = await response.blob();
const audioUrl = URL.createObjectURL(audioBlob);
const audio = new Audio(audioUrl);
audio.play();
```

### Example: Using Python

```python
import requests

url = "https://your-server.com/api/tts/speak"
headers = {
    "X-API-KEY": "your-api-key-here",
    "Content-Type": "application/json"
}
data = {
    "text": "Hello, this is a test message.",
    "voiceId": "edon"
}

response = requests.post(url, headers=headers, json=data)

if response.status_code == 200:
    with open("speech.wav", "wb") as f:
        f.write(response.content)
    print("Audio saved to speech.wav")
else:
    print(f"Error: {response.status_code}")
```

### Handling Multiline Text

You can include newlines in your text. They will be automatically converted to spaces with periods:

```json
{
  "text": "This is line one.\nThis is line two.\nThis is line three.",
  "voiceId": "edon"
}
```

### Error Responses

The API may return errors in these cases:

- **401 Unauthorized**: Invalid or missing API key
- **403 Forbidden**: 
  - API key has no remaining tokens
  - API key is disabled (for "once" type after first use)
- **400 Bad Request**: Invalid request format or missing fields
- **500 Internal Server Error**: Server error

---

## Iframe Embedding

Iframe embedding allows you to add a text-to-speech player directly to your website without writing custom code.

### Generating Embed Code

1. Go to the admin dashboard
2. Find the API key you want to use
3. Click **Iframe** next to the API key
4. You'll see:
   - **Iframe Code with Text** - Pre-filled with example text
   - **Iframe Code without Text** - User can enter text
   - **Iframe URL** - Direct URL to the player

### Using the Embed Code

#### Option 1: With Pre-filled Text

Copy the iframe code and paste it into your HTML:

```html
<iframe 
  src="https://your-server.com/embed/player?token=YOUR_TOKEN" 
  text_to_speech="Pershendetje, ky është zëri shqip." 
  width="500" 
  height="400" 
  frameborder="0">
</iframe>
```

The text will be automatically converted to speech when the iframe loads.

#### Option 2: User Input

Use the code without pre-filled text to let users enter their own text:

```html
<iframe 
  src="https://your-server.com/embed/player?token=YOUR_TOKEN" 
  width="500" 
  height="400" 
  frameborder="0">
</iframe>
```

Users can type text in the player and click play.

### Customizing the Player

Adjust the iframe attributes:

- **width**: Player width in pixels (e.g., `500`)
- **height**: Player height in pixels (e.g., `400`)
- **frameborder**: Border around iframe (`0` for no border, `1` for border)

### Security

- The API key is encoded in the token (not visible in the URL)
- Only domains specified in the API key's "Referer Domain" can embed the iframe
- Tokens expire after a set time for security

### Troubleshooting Iframe Issues

**Iframe not loading:**
- Check that your domain matches the "Referer Domain" set in the API key
- Verify the token is valid and not expired
- Check browser console for error messages

**Audio not playing:**
- Ensure the text attribute is set correctly
- Check that the API key has remaining tokens
- Verify network connectivity

---

## Viewing Usage History

The usage history page provides detailed information about API key usage.

### Accessing Usage History

1. Go to the admin dashboard
2. Find the API key you want to check
3. Click **History**

### History Page Overview

The history page shows:

#### API Key Information
- Owner name
- API key (masked - click "Show" to reveal)
- Remaining tokens and limit type

#### Summary Statistics
- **Total Tokens Used** - All-time token usage
- **Total Requests** - Number of API calls made
- **Tokens Used Today** - Tokens consumed today
- **Tokens Used This Month** - Tokens consumed this month
- **Remaining Tokens** - Available tokens (if limited)
- **Usage Percentage** - Percentage of limit used

#### Usage History Table

The table shows detailed information for each request:
- **Timestamp** - When the request was made
- **Endpoint** - API endpoint used
- **Method** - HTTP method (GET/POST)
- **Voice ID** - Voice used for generation
- **Tokens Used** - Tokens consumed
- **Cached** - Whether cached audio was used (Yes = 30% tokens, No = full tokens)
- **IP Address** - Client IP address (supports Cloudflare)

### Filtering History

Use filters to find specific records:

1. **Voice ID** - Filter by voice (`edon`, `arta`, `arben`, `dren`, or All)
2. **Cached** - Filter by cache status (Yes, No, or All)
3. **Endpoint** - Search by endpoint name (e.g., `/api/tts/speak`)
4. **IP Address** - Search by IP address
5. **Date From** - Start date for filtering
6. **Date To** - End date for filtering

Click **Apply Filters** to update the results, or **Clear** to reset.

### Pagination

If you have many history records:
- Use **Previous** and **Next** buttons to navigate pages
- Click page numbers to jump to specific pages
- The page shows "Showing X - Y of Z entries" at the bottom

---

## Token System Explained

### What Are Tokens?

Tokens are units used to measure text-to-speech usage. Each API request consumes tokens based on the text length.

### How Tokens Are Counted

Tokens are counted as **words longer than 3 characters** in your text.

**Examples**:
- "Hello world" → 2 tokens ("Hello" and "world" both count)
- "This is a test" → 2 tokens ("This" and "test" both count)
- "The quick brown fox" → 2 tokens ("quick" and "brown" count; "fox" is exactly 3 characters, so it doesn't count)

### Token Limit Types

#### Unlimited
- No restrictions on usage
- Use as much as you need
- Best for development or unlimited access scenarios

#### Total (One-time Limit)
- Set a total number of tokens
- Tokens decrease with each request
- When tokens reach zero, the API key stops working
- Best for one-time projects or fixed budgets

#### Monthly
- Tokens reset at the start of each month
- Usage resets automatically
- Best for regular monthly usage

#### Yearly
- Tokens reset at the start of each year
- Usage resets automatically
- Best for annual subscriptions or budgets

#### Once (Single Use)
- API key works only once
- After first successful request, the key is disabled
- Best for single-use scenarios or demos

### Cached Audio Discount

When audio is generated from cache (previously generated text), tokens cost only **30%** of the normal amount.

**Example**:
- First request: "Hello world" → 2 tokens ("Hello" and "world")
- Second request (same text): "Hello world" → 0.6 tokens (30% of 2, rounded up to 1)

This encourages reuse of common phrases and reduces costs.

### Checking Token Status

1. View the API key in the dashboard table
2. Check the **Token Limit** column for remaining tokens
3. Click **History** for detailed usage breakdown

### What Happens When Tokens Run Out?

- **Total/Once types**: API returns 403 Forbidden error
- **Monthly/Yearly types**: Wait until the next period reset
- **Unlimited**: Never runs out

---

## Frequently Asked Questions

### General Questions

**Q: How do I get started?**
A: Log in to the admin dashboard, create an API key, and start making API requests.

**Q: Can I use multiple API keys?**
A: Yes! Create separate API keys for different projects, environments, or clients.

**Q: What happens if I lose my API key?**
A: You can view API keys in the dashboard (click "Show" to reveal). If you've lost access to the dashboard, contact your administrator.

**Q: Can I change my admin password?**
A: Yes, click "Change Password" in the dashboard navigation.

### API Usage Questions

**Q: What's the maximum text length?**
A: There's no hard limit, but very long texts will consume more tokens and take longer to process.

**Q: Can I use special characters or emojis?**
A: Yes, the API supports Unicode characters. Emojis will be skipped in token counting.

**Q: How fast is the API?**
A: Response time depends on text length and server load. Cached audio returns almost instantly.

**Q: Can I use the API from mobile apps?**
A: Yes! The API works with any HTTP client - mobile apps, web apps, desktop applications, etc.

### Token Questions

**Q: Why did my tokens decrease more than expected?**
A: Check if the request used cached audio (30% discount) or if multiple requests were made.

**Q: When do monthly/yearly tokens reset?**
A: Monthly tokens reset at midnight on the 1st of each month. Yearly tokens reset on January 1st.

**Q: Can I increase my token limit?**
A: Yes, edit the API key and update the token limit value.

**Q: What happens to unused tokens?**
A: 
- **Total/Once**: Unused tokens are lost
- **Monthly**: Unused tokens reset each month
- **Yearly**: Unused tokens reset each year
- **Unlimited**: No tokens to track

### Iframe Questions

**Q: Can I customize the iframe player appearance?**
A: The player has a minimal design. You can adjust the iframe size using width and height attributes.

**Q: Can I use iframes on multiple domains?**
A: Set the "Referer Domain" to allow multiple domains, or create separate API keys for each domain.

**Q: Do iframes work on mobile devices?**
A: Yes, iframes work on mobile browsers. Ensure your website is mobile-responsive.

**Q: Can I pre-fill text in the iframe?**
A: Yes, use the `text_to_speech` attribute in the iframe tag.

### Troubleshooting

**Q: I'm getting 403 Forbidden errors**
A: 
- Check that your API key has remaining tokens
- Verify the API key is correct
- For "once" type keys, ensure it hasn't been used already

**Q: The audio sounds wrong or distorted**
A: 
- Try a different voice
- Check your text for special characters
- Ensure you're using the correct audio format (WAV)

**Q: Iframe won't load**
A: 
- Verify your domain matches the "Referer Domain" in API key settings
- Check that the token is valid
- Ensure your website allows iframes (check Content Security Policy)

**Q: How do I check my usage?**
A: Click "History" next to your API key in the dashboard to see detailed usage statistics.

---

## Support

For technical issues or questions not covered in this manual:

1. Check the usage history for error patterns
2. Review the API documentation at `/swagger-ui.html`
3. Contact your system administrator
4. Check server logs for detailed error messages

---

## Quick Reference

### Dashboard URLs
- **Admin Dashboard**: `/admin`
- **Change Password**: `/admin/password`
- **API Key History**: `/admin/api-keys/{id}/history`
- **Iframe Generator**: `/admin/api-keys/{id}/iframe`

### API Endpoints
- **Generate Speech**: `POST /api/tts/speak`
- **Iframe Player**: `GET /embed/player?token={token}`
- **API Documentation**: `/swagger-ui.html`

### Available Voices
- `edon` (default)
- `arta`
- `arben`
- `dren`

---

**Last Updated**: February 2026
