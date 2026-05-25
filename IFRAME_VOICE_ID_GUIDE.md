# Iframe Voice ID Documentation

This guide explains how to specify the voice ID when embedding the Flossk TTS player in an iframe.

## Overview

The Flossk TTS iframe player supports multiple voices. You can specify which voice to use either:
1. **Via iframe attributes** (recommended for pre-filled text)
2. **Via user selection** (when users can enter their own text)

## Available Voices

- **edon** - Default voice (used if no voice is specified)
- **arta** - Alternative voice
- **arben** - Alternative voice
- **dren** - Alternative voice

## Method 1: Using Iframe Attributes (Recommended)

You can specify the voice ID directly in the iframe tag using one of these attributes:

### Supported Voice ID Attribute

- `voice_id` - Voice ID to use

### Example: Pre-filled Text with Voice ID

```html
<iframe 
  src="https://your-server.com/embed/player?token=YOUR_TOKEN" 
  text_to_speech="Pershendetje, ky është zëri shqip."
  voice_id="arta"
  width="500" 
  height="400" 
  frameborder="0">
</iframe>
```


### Complete Example with All Attributes

```html
<iframe 
  src="https://your-server.com/embed/player?token=YOUR_TOKEN" 
  text_to_speech="Pershendetje, ky është zëri shqip."
  voice_id="arta"
  width="500" 
  height="400" 
  frameborder="0">
</iframe>
```

**What happens:**
1. The text "Pershendetje, ky është zëri shqip." is automatically loaded
2. The voice "arta" is automatically selected
3. Audio is generated immediately when the iframe loads
4. The audio player appears automatically

## Method 2: User Selection (No Pre-filled Voice)

If you don't specify a voice ID attribute, users can select the voice from a dropdown:

```html
<iframe 
  src="https://your-server.com/embed/player?token=YOUR_TOKEN" 
  text_to_speech="Enter your text here"
  width="500" 
  height="400" 
  frameborder="0">
</iframe>
```

**What happens:**
1. The text input field is pre-filled (if `text_to_speech` is provided)
2. The voice selector defaults to "edon"
3. Users can change the voice before generating audio
4. Users can modify the text if needed

## Method 3: User Input Only (No Pre-filled Content)

If you don't provide any attributes, users can enter text and select voice:

```html
<iframe 
  src="https://your-server.com/embed/player?token=YOUR_TOKEN" 
  width="500" 
  height="400" 
  frameborder="0">
</iframe>
```

**What happens:**
1. Text input field appears
2. Voice selector appears (defaults to "edon")
3. Users enter text and select voice
4. Users click "Gjenero Audio" to generate

## Supported Text Attribute

The iframe supports this text attribute:

- `text_to_speech` - Text to convert to speech

## Complete Attribute Reference

| Attribute | Description | Required | Example Values |
|-----------|-------------|----------|----------------|
| `text_to_speech` | Text to convert to speech | No | "Hello world" |
| `voice_id` | Voice ID to use | No | `edon`, `arta`, `arben`, `dren` |

## Usage Examples

### Example 1: Albanian Text with Arta Voice

```html
<iframe 
  src="https://tts.example.com/embed/player?token=abc123xyz" 
  text_to_speech="Mirëdita, si jeni?"
  voice_id="arta"
  width="100%" 
  height="400" 
  frameborder="0">
</iframe>
```

### Example 2: Multiple Iframes with Different Voices

```html
<!-- First iframe with Edon voice -->
<iframe 
  src="https://tts.example.com/embed/player?token=abc123xyz" 
  text_to_speech="This is Edon speaking."
  voice_id="edon"
  width="500" 
  height="400" 
  frameborder="0">
</iframe>

<!-- Second iframe with Arben voice -->
<iframe 
  src="https://tts.example.com/embed/player?token=abc123xyz" 
  text_to_speech="This is Arben speaking."
  voice_id="arben"
  width="500" 
  height="400" 
  frameborder="0">
</iframe>
```

### Example 3: Dynamic Voice Selection (JavaScript)

```html
<script>
function createTTSIframe(text, voiceId) {
  const iframe = document.createElement('iframe');
  iframe.src = 'https://tts.example.com/embed/player?token=YOUR_TOKEN';
  iframe.setAttribute('text_to_speech', text);
  iframe.setAttribute('voice_id', voiceId);
  iframe.width = '500';
  iframe.height = '400';
  iframe.frameBorder = '0';
  return iframe;
}

// Create iframe with specific voice
const container = document.getElementById('tts-container');
const iframe = createTTSIframe('Hello, this is a test.', 'dren');
container.appendChild(iframe);
</script>
```

### Example 4: React Component

```jsx
function TTSPlayer({ text, voiceId = 'edon', token }) {
  return (
    <iframe
      src={`https://tts.example.com/embed/player?token=${token}`}
      text_to_speech={text}
      voice_id={voiceId}
      width="500"
      height="400"
      frameBorder="0"
    />
  );
}

// Usage
<TTSPlayer 
  text="Hello from React" 
  voiceId="arta" 
  token="your-token-here" 
/>
```

### Example 5: Vue.js Component

```vue
<template>
  <iframe
    :src="`https://tts.example.com/embed/player?token=${token}`"
    :text_to_speech="text"
    :voice_id="voiceId"
    width="500"
    height="400"
    frameborder="0"
  />
</template>

<script>
export default {
  props: {
    text: String,
    voiceId: {
      type: String,
      default: 'edon'
    },
    token: String
  }
}
</script>
```

## Behavior Details

### Voice ID Validation

- If an invalid voice ID is provided, it defaults to `edon`
- Valid voice IDs are case-insensitive (`EDON`, `Edon`, `edon` all work)
- If no voice ID is provided, `edon` is used as default

### Text and Voice Interaction

- If `text_to_speech` is provided **without** `voice_id`:
  - Text is pre-filled
  - Voice defaults to `edon`
  - Audio generates automatically
  - User can still change voice and regenerate

- If `voice_id` is provided **without** `text_to_speech`:
  - Voice is pre-selected
  - Text input appears for user entry
  - User enters text and generates audio

- If **both** are provided:
  - Text is pre-filled
  - Voice is pre-selected
  - Audio generates automatically with specified voice

- If **neither** is provided:
  - Text input appears
  - Voice selector appears (defaults to `edon`)
  - User enters text, selects voice, and generates

## Troubleshooting

### Voice Not Changing

**Issue**: Voice ID attribute is ignored

**Solutions**:
1. Check attribute name spelling (must be `voice_id`)
2. Verify voice ID is valid (`edon`, `arta`, `arben`, or `dren`)
3. Check browser console for errors
4. Ensure iframe has loaded completely before checking

### Default Voice Always Used

**Issue**: Specified voice ID is not applied

**Solutions**:
1. Verify the voice ID attribute is correctly set
2. Check that the voice ID value is lowercase (or matches exactly)
3. Clear browser cache and reload
4. Test with different voice IDs to verify functionality

### Voice Selector Not Showing

**Issue**: Voice selector dropdown doesn't appear

**Solutions**:
1. This is normal if `text_to_speech` is provided - audio generates automatically
2. If you want the selector, don't provide `text_to_speech` attribute
3. Check iframe height is sufficient (minimum 400px recommended)

## Best Practices

1. **Always specify voice_id when using pre-filled text** - Ensures consistent voice across all instances
2. **Use lowercase voice IDs** - More reliable across different browsers
3. **Set appropriate iframe dimensions** - Minimum 400px height recommended
4. **Test voice selection** - Verify the correct voice is used before deploying
5. **Use correct attribute names** - `text_to_speech` and `voice_id` (snake_case)

## Quick Reference

```html
<!-- Minimal example with voice -->
<iframe 
  src="https://tts.example.com/embed/player?token=TOKEN" 
  text_to_speech="Your text"
  voice_id="arta"
  width="500" 
  height="400">
</iframe>

<!-- All supported attributes -->
<iframe 
  src="https://tts.example.com/embed/player?token=TOKEN" 
  text_to_speech="Your text"
  voice_id="edon"
  width="500" 
  height="400" 
  frameborder="0">
</iframe>
```

## See Also

- [User Manual](./USER_MANUAL.md) - Complete user guide
- [Production Setup](./PRODUCTION_SETUP.md) - Deployment instructions
- [Cloudflare Setup](./CLOUDFLARE_SETUP.md) - Cloudflare configuration
