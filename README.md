# Atlas

## <a name="overview"></a>Overview

Atlas is an open source framework of customizable UI components for use with the Layer SDK designed to get messaging tested and integrated quickly.  This repository contains the Atlas library.  For a fully-featured messaging app, see the open source [Atlas Messenger](https://github.com/layerhq/Atlas-Android-Messenger) project, which uses this Atlas library and the Layer SDK.

#### Requirements

Atlas requires Android API Level >= 14 (OS v4.0). The Layer SDK version requirements for each release are tightly coupled. See the release notes for details about specifics.

## <a name="key_concepts"></a>Key Concepts
With Atlas, Messages have types.  One type might be rich text, and another might be a map location or photo.  Anything that can be packaged into a set of MIME Types and data can be represented by Atlas.

Under the hood, <a href="layer-atlas/src/main/java/com/layer/atlas/messagetypes/MessageSender.java">MessageSenders</a> send individual Message types, and <a href="layer-atlas/src/main/java/com/layer/atlas/messagetypes/AtlasCellFactory.java">AtlasCellFactories</a> render them.  Additional Message types can be added to your app by extending these classes.  For a list of default types, see the <a href="layer-atlas/src/main/java/com/layer/atlas/messagetypes">messagetypes</a> subpackage.

## <a name="api_quickstart"></a>API Quickstart
The Atlas library is located in the `layer-atlas` directory.  The table below details the most important classes in Atlas and is hyperlinked directly to the current java file.

<table>
    <tr><th colspan="2" style="text-align:center;">Views</th></tr>
    <tr>
        <td><a href="layer-atlas/src/main/java/com/layer/atlas/AtlasConversationsRecyclerView.java">AtlasConversationsRecyclerView</a></td>
        <td>A list of Conversations</td>
    </tr>
    <tr>
        <td><a href="layer-atlas/src/main/java/com/layer/atlas/AtlasMessagesRecyclerView.java">AtlasMessagesRecyclerView</a></td>
        <td>A list of Messages within a Conversation</td>
    </tr>
    <tr>
        <td><a href="layer-atlas/src/main/java/com/layer/atlas/AtlasMessageComposer.java">AtlasMessageComposer</a></td>
        <td>A View used to compose and send Messages</td>
    </tr>
    <tr>
        <td><a href="layer-atlas/src/main/java/com/layer/atlas/AtlasAddressBar.java">AtlasAddressBar</a></td>
        <td>Participant selection with dynamic filtering</td>
    </tr>
    <tr>
        <td><a href="layer-atlas/src/main/java/com/layer/atlas/AtlasTypingIndicator.java">AtlasTypingIndicator</a></td>
        <td>Displays TypingIndicator information for a Conversation</td>
    </tr>
    <tr><th colspan="2" style="text-align:center;">Factories and Senders</th></tr>
    <tr>
        <td><a href="layer-atlas/src/main/java/com/layer/atlas/messagetypes/AtlasCellFactory.java">AtlasCellFactory</a></td>
        <td>Classifies, parses, and renders Messages</td>
    </tr>
    <tr>
        <td><a href="layer-atlas/src/main/java/com/layer/atlas/messagetypes/MessageSender.java">MessageSender</a></td>
        <td>Sends Messages</td>
    </tr>
    <tr>
        <td><a href="layer-atlas/src/main/java/com/layer/atlas/AtlasTypingIndicator.java">AtlasTypingIndicator. TypingIndicatorFactory</a></td>
        <td>Renders typing indicators</td>
    </tr>
</table>

## <a name="installation"></a>Installation

Add the following to the `build.gradle`:
```groovy
repositories {
    maven { url "https://raw.githubusercontent.com/layerhq/releases-android/master/releases/" }
    maven { url "https://raw.githubusercontent.com/layerhq/Atlas-Android/master/releases/" }
}

dependencies {
    compile 'com.layer.atlas:layer-atlas:0.4.15'
}
```

### <a name="libraries"></a>Libraries

Atlas uses [Picasso](https://github.com/square/picasso) for image caching, resizing, and processing, and [Subsampling Scale Image View](https://github.com/davemorrissey/subsampling-scale-image-view) for image its in-app lightbox.  Other dependencies include the Android `recyclerview`, `appcompat`, and `design` libraries.

## <a name="component_details"></a>Component Details
Atlas is divided into five basic `View` components, typically presented on a screen with a user's [conversations](#conversations), a screen with [messages](#messages) within a conversation, and a component that lets the user select [participants](#participants).

### <a name="conversations"></a>Conversations

#### AtlasConversationsRecyclerView

The <a href="layer-atlas/src/main/java/com/layer/atlas/AtlasConversationsRecyclerView.java">AtlasConversationsRecyclerView</a> is a list of Conversations.

##### XML

```xml
<com.layer.atlas.AtlasConversationsRecyclerView
    android:id="@+id/conversations_list"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    />
```

##### Java

```java
conversationsList = ((AtlasConversationsRecyclerView) findViewById(R.id.conversations_list))
	.init(layerClient, picasso)
	.setOnConversationClickListener(new OnConversationClickListener() {
		public void onConversationClick(AtlasConversationsAdapter adapter, Conversation conversation) {
			launchMessagesList(conversation);
		}
		
		public boolean onConversationLongClick(AtlasConversationsAdapter adapter, Conversation conversation) {
		    return false;
		}
	})
    .addCellFactories(new TextCellFactory(), 
    new ThreePartImageCellFactory(layerClient, picasso),
    new LocationCellFactory(picasso),
    new SinglePartImageCellFactory(layerClient, picasso));
```

### <a name="messages"></a>Messages

#### AtlasMessagesRecyclerView

The <a href="layer-atlas/src/main/java/com/layer/atlas/AtlasMessagesRecyclerView.java">AtlasMessagesRecyclerView</a> is list of Messages, rendered by <a href="layer-atlas/src/main/java/com/layer/atlas/messagetypes/AtlasCellFactory.java">AtlasCellFactories</a>.

##### XML

```xml
<com.layer.atlas.AtlasMessagesRecyclerView
    android:id="@+id/messages_list"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    />
```

##### Java

```java
messagesList = ((AtlasMessagesRecyclerView) findViewById(R.id.messages_list))
	.init(layerClient, picasso)
	.setConversation(conversation)
	.addCellFactories(
		new TextCellFactory(),
		new ThreePartImageCellFactory(this, layerClient, picasso),
		new LocationCellFactory(this, picasso));
```

#### AtlasMessageComposer

The <a href="layer-atlas/src/main/java/com/layer/atlas/AtlasMessageComposer.java">AtlasMessageComposer</a> is a text entry area for composing messages and a menu of <a href="layer-atlas/src/main/java/com/layer/atlas/messagetypes/AttachmentSender.java">AttachmentSenders</a>. 

##### XML

```xml
<com.layer.atlas.AtlasMessageComposer
    android:id="@+id/message_composer"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    />
```

##### Java

```java
messageComposer = ((AtlasMessageComposer) findViewById(R.id.message_composer))
	.init(layerClient)
        .setConversation(conversation)
	.setTextSender(new TextSender())
	.addAttachmentSenders(
		new CameraSender("Camera", R.drawable.ic_photo_camera_white_24dp, this, getApplicationContext().getPackageName() + ".file_provider"),
		new GallerySender("Gallery", R.drawable.ic_photo_white_24dp, this),
		new LocationSender("Location", R.drawable.ic_place_white_24dp, this));
```

#### AtlasTypingIndicator

The <a href="layer-atlas/src/main/java/com/layer/atlas/AtlasTypingIndicator.java">AtlasTypingIndicator</a> presents the user with active typists.

##### XML

```xml
<com.layer.atlas.AtlasTypingIndicator
    android:id="@+id/typing_indicator"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    />
```

##### Java

```java
typingIndicator = new AtlasTypingIndicator(this)
	.init(layerClient)
	.setTypingIndicatorFactory(new BubbleTypingIndicatorFactory())
	.setTypingActivityListener(new AtlasTypingIndicator.TypingActivityListener() {
		public void onTypingActivityChange(AtlasTypingIndicator typingIndicator, boolean active) {
			messagesList.setFooterView(active ? typingIndicator : null);
		}
	});
```

### <a name="Message Types"></a>Message Types
By default, Atlas supports the following types of messages.

| Type            | Description |
| ----------------|-------------|
| Generic         | Default handler for unknown message types. Displays the mimetype and the content size |
| Text            | Handler for `text/plain` content. |
| Location        | Handler for `location/coordinate` content. Given `lat`/`lon` information, displays the location image (from Google maps), with a hyperlink that launches Maps application|
| ThreePartImage  | Handler for 3 part JPEG image, with preview & dimensions. By default, displays the preview image. On tap, downloads and renders the full resolution image |
| SinglePartImage | Handler for any mime type that starts with `image` tag |

We expect to add support for other handlers in future. If you would like to build a handler, please check <a href="docs/Message-Handlers.md">doc on message handlers</a>.

### <a name="Identity"></a>Identity

An application server can directly upload user information to Layer server. This user information is called <a href="https://docs.layer.com/sdk/android/identities">Identity</a>. `AtlasAddressBar` and `AtlasAvatar` are controls that are used to render the Identity information.

#### AtlasAddressBar
`AtlasAddressBar` can be used to show a list of users. For eg, the list of users in a `Conversation` or to show a user list for creating a new `Conversation`.

##### XML

```xml
<com.layer.atlas.AtlasAddressBar
    android:id="@+id/address_bar"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    />
```

##### Java

```java
addressBar = (AtlasAddressBar) findViewById(R.id.address_bar)
	.init(layerClient, picasso)
	.setOnConversationClickListener(new OnConversationClickListenertener() {
		public void onConversationClick(AtlasAddressBar addressBar, Conversation conversation) {
			setConversation(conversation);
		}
	})
	.setOnParticipantSelectionChangeListener(new OnParticipantSelectionChangeListener() {
		public void onParticipantSelectionChanged(AtlasAddressBar addressBar, List<Identity> participants) {
			if (participants.isEmpty()) {
				setConversation(null);
				return;
			}
			try {
				ConversationOptions options = new ConversationOptions().distinct(true);
				setConversation(layerClient.newConversation(options, new HashSet<>(participants)), false);
			} catch (LayerConversationException e) {
				setConversation(e.getConversation(), false);
			}
		}
	});
```

#### AtlasAvatar
`AtlasAvatar`can be used to show information about one user, or as a cluster of multiple users. `AtlasAvatar` uses [Picasso](https://github.com/square/picasso) to render the avatar image. So, you need to `init` 

##### XML

```xml
        <com.layer.atlas.AtlasAvatar
            android:id="@+id/avatar"
            android:layout_width="@dimen/atlas_avatar_item_single"
            android:layout_height="@dimen/atlas_avatar_item_single"
            android:layout_margin="@dimen/atlas_padding_normal"/>
```

##### Java

```java
	    // To create an avatar
            mAvatarCluster = (AtlasAvatar) itemView.findViewById(R.id.avatar);
	    
	    // To initialize Picasso
	    viewHolder.mAvatarCluster
		.init(mPicasso)
		.setStyle(conversationStyle.getAvatarStyle());
		
	    // To set identites meant for the avatar cluster
	    HashSet<Identity> participants = new HashSet<>(conversation.getParticipants());
	    viewHolder.mAvatarCluster.setParticipants(participants);
```

## <a name="contributing"></a>Contributing
Atlas is an Open Source project maintained by Layer. Feedback and contributions are always welcome and the maintainers try to process patches as quickly as possible. Feel free to open up a Pull Request or Issue on Github.

## <a name="license"></a>License

Atlas is licensed under the terms of the [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). Please see the [LICENSE](LICENSE) file for full details.

## <a name="contact"></a>Contact

Atlas was developed in San Francisco by the Layer team. If you have any technical questions or concerns about this project feel free to reach out to [Layer Support](mailto:support@layer.com).

### <a name="credits"></a>Credits

* [Amar Srinivasan](https://github.com/sriamar)
* [Steven Jones](https://github.com/sjones94549)
* [Peter Elliott](https://github.com/smpete)
