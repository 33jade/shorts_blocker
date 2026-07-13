# ShortBlocker Privacy Policy

Last updated: 2026-07-12

This Privacy Policy explains how ShortBlocker handles data. It is intended for publication as the privacy policy linked from the app and the store listing.

## 1. App Purpose

ShortBlocker helps reduce long viewing sessions on YouTube Shorts. The app uses Android AccessibilityService only to detect YouTube Shorts playback screens and leave those screens according to the user's settings.

## 2. Developer and Contact

- App name: ShortBlocker
- Developer: See the Google Play store listing.
- Privacy contact: See the Google Play store listing.

The developer name and privacy contact are provided through the Google Play store listing.

## 3. Data Accessed by the App

When the AccessibilityService is enabled and the user has provided explicit consent, the app may access the following information from the YouTube app screen:

- Screen structure
- View IDs
- Text visible to the accessibility tree
- Content descriptions visible to the accessibility tree

This information is used only to determine whether the current YouTube screen is a Shorts playback screen.

## 4. Data Stored on the Device

The app stores the following settings locally on the device using Android DataStore:

- Whether Shorts blocking is enabled
- The accepted consent version
- Daily Shorts allowance setting
- Today's used allowance time
- Temporary unblock expiration time
- Whether the block explanation screen is enabled

These values are used to apply the user's blocking, allowance, and temporary unblock settings.

## 5. Data Collection and Sharing

ShortBlocker does not collect, transmit, sell, or share personal data with external servers, developers, advertisers, analytics providers, or other third parties.

The app does not use accounts, advertising identifiers, analytics SDKs, crash reporting SDKs, or external tracking SDKs.

## 6. Logs

Debug builds may write YouTube screen node IDs or other diagnostic messages to local Android logcat for development and verification. These logs are not transmitted by the app. Release builds do not output normal operation logs or development-only screen structure logs. Release builds keep only error logs for unexpected failures.

## 7. Data Retention and Deletion

Settings and allowance usage are retained locally until the user clears the app data or uninstalls the app.

To delete locally stored data, the user can:

- Clear the app's storage from Android system settings, or
- Uninstall the app.

ShortBlocker does not create a user account, so there is no account deletion process.

## 8. Security

The app stores settings in app-private local storage. Because the app does not transmit user data to external servers, it does not send personal data over a network.

## 9. AccessibilityService Disclosure

ShortBlocker uses AccessibilityService for the following purposes:

- Detecting YouTube Shorts playback screens
- Automatically leaving Shorts screens when blocking is active
- Respecting daily allowance and temporary unblock settings
- Showing a block explanation screen after automatic leaving

The AccessibilityService is not used to read passwords, payment information, private messages, or content from other apps for profiling, advertising, or tracking.

## 10. Changes to This Policy

If the app's data access, storage, sharing, or AccessibilityService behavior changes, this Privacy Policy and the in-app disclosure will be updated. If the consent disclosure changes materially, the app will require consent to the updated version before analysis or automatic actions resume.

## Publication Checklist

Before publishing, confirm that the developer name and privacy contact above match the Google Play store listing.
