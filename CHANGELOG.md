# Atlas Android

## 0.4.18

### Major Changes
  * Update to Layer Android SDK Version 0.23.23

## 0.4.17

#### Bug Fixes
  * Fix Picasso crash when size of image is 0
  * Fix default attach button visibility

## 0.4.16

### Major Changes
  * Update to Layer Android SDK Version 0.23.22

## 0.4.15

### Major Changes
  * Update to Layer Android SDK Version 0.23.21
  
## 0.4.14

### Major Changes
  * Update to Layer Android SDK Version 0.23.20
  
## 0.4.13

### Major Changes
  * Update to Layer Android SDK Version 0.23.19

## 0.4.12

### Major Changes
  * Update to Layer Android SDK Version 0.23.18
  
## 0.4.11

### Major Changes
  * Update to Layer Android SDK Version 0.23.17
  * Added read/unread styling for DateTextView
  * Added getter/setter for avatar presence to the ConversationsRecyclerView

## 0.4.10

### Major Changes
  * Update to Layer Android SDK Version 0.23.16

## 0.4.9

### Major Changes
  * Added support for Telemetry. For more details please refer to [Android Documentation](https://docs.layer.com/sdk/android/privacy#telemetry)
  * Displaying Timestamp for the first message in the list
  * Basic tests
  * Tests now run on CircleCI with every pull request
  
#### Bug Fixes
  * Fixed chat bubble empty on receiving long messages
  * Using fixed heights suitable for image previews to stop images from resizing on fragment/activity resize

## 0.4.8 -- ROLLED BACK due to bug. please update to 0.4.9

## 0.4.7

### Major Changes
  * Update to Layer Android SDK Version 0.23.13
  * Add support for Presence

## 0.4.6

### Major Changes
  * Update to Layer Android SDK Version 0.23.12
  * Update Google Play Services to 10.2.0
  
## 0.4.5

### Major Changes
  * Update to Layer Android SDK Version 0.23.11
  
### Bug Fixes
  * Fixing avatars in group conversations (#156)
  
## 0.4.4

### Major Changes
  * Expose `setShouldShowAvatarInOneOnOneConversations` & `getShouldShowAvatarInOneOnOneConversations` on `AtlasMessagesRecyclerView`
  to allow the showing of the "other" participant's Avatar in a conversation with only 2 participants.

## 0.4.3 : ROLLED BACK due to a bug. please update to 0.4.4

## 0.4.2

### Major Changes
  * Update to Layer Android SDK Version 0.23.8
  
## 0.4.1

### Major Changes
  * Update to Layer Android SDK Version 0.23.7
  
### Bug Fixes
  * Fixed memory leak in `LocationSender`
  * Fixed crash when a ThreePartImage's preview and image are the same size
  * Fixed manifest merge issues + crashes caused by workarounds by removing the FileProvider required for CameraSender.
  
## 0.4.0

### Major Changes
  * Update to Layer Android SDK Version 0.23.6
  * Added log markers for performance testing

### Bug Fixes
  * Fixed manifest merge issues + crashes caused by workarounds by removing the FileProvider required for CameraSender.

### Migration Guide
####  `CameraSender`
  * Developers are now required to implement their own FileProvider and supply the CameraSender with the provider authority.
  * Refer to `/docs/CameraSender.md` for details on how to implement a FileProvider

## 0.3.7

### Bug fixes
  * Fixed crash when participant was removed from the conversation (APPS-2719)

## 0.3.6

### Features
  * Added convenience methods for checking and requesting permissions in `AttachmentSender`
  * Fix crash while using the Camera to take a photo on API Level 24 (Android 7.0 - Nougat)
  * Fix crash while using the Gallery to upload a photo on API Level 24 (Android 7.0 - Nougat)

## 0.3.5

### Major Changes
  * Update to Layer Android SDK Version 0.23.4

## 0.3.4

### Features
  * No longer need to fork the project to create custom cell factories. Subclass `AtlasCellFactory` and register an instance with `AtlasConversationsAdapter` and `AtlasMessagesAdapter`.
  * `ThreePartImageCellFactory`, `LocationCellFactory` and `SinglePartImageCellFactory` no longer require an `Activity` or `Context` to be supplied to the constructor. Those constructors are now deprecated. 

## 0.3.3

### Major Changes
  * Update to Layer Android SDK Version 0.23.2

## 0.3.2

### Major Changes
  * Update to Layer Android SDK Version 0.23.1

## 0.3.1

### Features
  * Updating to Layer SDK 0.23.0 (FCM support)
  * Compile and target SDK set to API 24
  * Support library versions are now 24.2.1
  * Google play services location is updated to 9.6.1

### Migration Guide
  * There are no API changes in Atlas directly. Please follow the Migration Guide for Layer SDK 0.23.0.

## 0.3.0

### Features
  * Support for Identities

### Bug fixes
  * Message receipts are now only used on the latest message for the current user. This increases performance on the `AtlasMessagesRecyclerView` when loaded with 1000+ messages, as well as achieves parity with iOS. (APPS-2525)
  * Fixed last message view cropped when build target was SDK 24.

### Migration Guide

`Participant` and `ParticipantProvider` classes have been removed as `Identity` objects now contain user data.

#### `AtlasAddressBar`
  * No longer need to pass a `ParticipantProvider` on `init()`.
  * `getSelectedParticipants()` now returns a set of `Identity` objects rather than user IDs.
  * `setSelectedParticipants()` now takes a set of `Identity` objects as an argument rather than user IDs.
  * `OnParticipantSelectionChangeListener.onParticipantSelectionChanged()` now passes a list of `Identity` objects rather than user IDs.

#### `AtlasAvatar`
  * No longer need to pass a `ParticipantProvider` on `init()`.
  * `setParticipants(...)` now takes `Identity` objects as arguments rather than user IDs.
  * `getParticipants()` now returns a set of `Identity` objects rather than user IDs.
  * The image URL used is `Identity.getAvatarImageUrl()`

#### `AtlasConversationsRecyclerView`
  * No longer need to pass a `ParticipantProvider` on `init()`.
  * MUST call `onDestroy()` in your Activity's or Fragment's `onDestroy()` to unregister event listeners on `LayerClient`.

#### `AtlasMessageComposer`
  * No longer need to pass a `ParticipantProvider` on `init()`.

#### `AtlasMessagesRecyclerView`
  * No longer need to pass a `ParticipantProvider` on `init()`.
  * MUST call `onDestroy()` in your Activity's or Fragment's `onDestroy()` to unregister event listeners on `LayerClient`.

#### `AtlasTypingIndicator`
  * The method signature of `onTypingIndicator()` has changed. It now passes a full `Identity` object as an argument rather than a user ID.
  * The method signature of `TypingIndicatorFactory.onBindView()` has changed. The map is now keyed by `Identity` objects rather than user IDs. Classes that implement this interface will need to be updated.

#### `AtlasConversationsAdapter`
  * Changes here are only relevant if using this directly without using a `AtlasConversationsRecyclerView`.
  * No longer need to pass a `ParticipantProvider` in the constructors.
  * MUST call `onDestroy()` in your Activity's or Fragment's `onDestroy()` to unregister event listeners on `LayerClient`.

#### `AtlasMessagesAdapter`
  * Changes here are only relevant if using this directly without using a `AtlasMessagesRecyclerView`.
  * No longer need to pass a `ParticipantProvider` in the constructor.
  * MUST call `onDestroy()` in your Activity's or Fragment's `onDestroy()` to unregister event listeners on `LayerClient`.

#### `AtlasCellFactory`
  * Any custom cell factories will need to be updated.
  * No longer need to pass a `ParticipantProvider` on `parseContent()`.
  * No longer need to pass a `ParticipantProvider` on `getParsedContent()`.

#### `MessageSender`
  * No longer need to pass a `ParticipantProvider` on `init()`.
  * Usage of subclasses (`AttachmentSender`, `TextSender`, etc) will need to be updated.

#### `AvatarTypingIndicatorFactory`
  * No longer need to pass a `ParticipantProvider` on `init()`.

#### `Util`
  * No longer need to pass a `ParticipantProvider` on `getConversationTitle()`.
  * 'getInitials()' now takes an `Identity` object rather than a `Participant` object.

## 0.2.14

### Major Changes
  * Updating to Google Play Services 9.2.0

### Bug Fixes
  * Attachment sender background is set even when an Atlas theme is not used (APPS-2536)

## 0.2.13

### Major Changes
  * Update to Layer Android SDK Version 0.21.3

## 0.2.12

### Major Changes
  * Update to Layer Android SDK Version 0.21.2
  
## 0.2.11

### Major Changes
  * Renamed `Log.setAlwaysLoggable` to `Log.setLoggingEnabled`
  * Updated to Layer Android SDK Version 0.21.1

### Bug Fixes
  * A `ViewPager` can now contain `AtlasConversationsRecyclerView` without a manual `refresh()`
    call (APPS-2444)

## 0.2.10

### Major Changes
  * Updated to Layer Android SDK Version 0.21.0
  * Removed `Util.waitForContent` as that is now supported in the Layer SDK
  * Publishing AAR so it can be included via Maven

### Features
  * Allowing customization of attachment menu background via `attachmentSendersBackground`

## 0.2.9

### Major Changes
  * Updated to Layer Android SDK Version 0.20.4

## 0.2.8

### Major Changes
  * Updated to Layer Android SDK Version 0.20.3

## 0.2.7

### Major Changes
  * Updated to Layer Android SDK Version 0.20.2

## 0.2.6

### Major Changes
  * Updated to Layer Android SDK Version 0.20.1

## 0.2.5

### Features
  * Updated to Layer Android SDK Version 0.20.0 with support for `ALL_MY_DEVICES` deletion.
  * Remove requirement for camera permission

## 0.2.4

### Features
  * Updated to Android API 23 for `compileSdkVersion` and `targetSdkVersion`.
  * Added dynamic permission handling to AttachmentSenders.


## 0.2.3

### Features
  * Added styling through XML attributes ([issue #28](https://github.com/layerhq/Atlas-Android/issues/28)).


## 0.2.2

### Features
  * Added `ContentLoadingProgressBar` for image and location cells, as well as the image popup ([issue #32](https://github.com/layerhq/Atlas-Android/issues/32)).
  * `TextCellFactory` parses text for clickable links, emails, addresses, and phone numbers.


## 0.2.1

### Features
  * Added `MessageSender.Callback` for receiving events when sending `Messages` ([issue #33](https://github.com/layerhq/Atlas-Android/issues/33)).
  * Added `AtlasMessageComposer.setMessageSenderCallback(Callback)` for handling sender callbacks in
    aggregate.


## 0.2.0

`0.2.0` was a complete rewrite of the initial Atlas Android preview.  The `0.2.0` APIs are expected to be stable.

### Major Changes
  * Messages are rendered by [AtlasCellFactories](https://github.com/layerhq/Atlas-Android/blob/master/layer-atlas/src/main/java/com/layer/atlas/messagetypes/AtlasCellFactory.java).
  * Messages are sent by [MessageSenders](https://github.com/layerhq/Atlas-Android/blob/master/layer-atlas/src/main/java/com/layer/atlas/messagetypes/MessageSender.java).
  * [Picasso](https://github.com/square/picasso) is now used for image caching and manipulating instead of the Atlas.ImageLoader class.


## 0.1.0

Initial Atlas Android preview.
