# Message Handlers

Atlas provides a few default message handlers. If you would like to build your own, here are the steps to follow.

## <a name="overview"></a>Overview
Layer SDK supports sending messages with multiple message parts. Each message part can be as large as 2Gb. So, the underlying sdk is capable of supporting various types of message data. Currently, Atlas only supports a few of these data types. 

First, Atlas determines the type of handler to use based on all the mimetypes in a message. So, you should assume a `1 to 1` mapping from a `Message` to a handler. The message handlers in Atlas is defined as `AtlasCellFactory`. 

Second, sending or receiving large content takes time, uses bandwidth, and more time to render. So, along with the actual content, consider generating, and storing a preview, with other information that would help make the overall user experience better. For an example, check `ThreePartImage`.

Third, if you build a custom handler, consider how it will work in other devices. Specifically, ensure whatever you are building is compatible and works well on iOS devices, Windows, Macs, and various browsers.

Fourth, in order to build a new handler, you could follow the one of the existing examples like `ThreePartImage`


## <a name="steps"></a>Steps

Here are the steps involved in building a new handler

1. Create a new handler for your custom message type by extending `AtlasCellFactory`.
2. Create a new sender for your custom message type by extending `MessageSender`.

3. Add the new cell factory to your `AtlasConversationRecyclerView` and `AtlasMessagesRecyclerView`
    * The order in which you register factories is important. The first matching factory will be used for displaying the message and its previews.
    * Use the same order for both `AtlasConversationRecyclerView` and `AtlasMessagesRecyclerView`.
4. If needed, add the new mime-types to `autoDownloadMimeTypes` option in `LayerClient.Options`.

## <a name="gif"></a>GIF Handler Example

In case of GIF, first you have to figure out what is the best way to store and transmit data. For this discussion, 

* Let's assume you want to store the original gif data as a message part. 
* Let's also assume you want to store a preview part (that shows the first frame?), and 
* You need to store some form of dimension/info about the gif. For better user experience, your preview should be same dimension as original. So, you will need to store only one dimension information. This leads to something similar to ThreePartImage support we have in Atlas. When thinking about this from Atlas perspective, you need to handle 2 high level concepts. Sending a gif, and receiving a gif.
      
### Sending a GIF

Ensure your ThreePartGifUtils creates the 3 part gif, and it is sent. You can follow the code pattern we use for image

* Sending a three part image from gallery : https://github.com/layerhq/Atlas-Android/blob/44b32948858a6ee3c9842eff8c899a88ac7f8945/layer-atlas/src/main/java/com/layer/atlas/messagetypes/threepartimage/GallerySender.java#L92

* Creating a three part image in utils : https://github.com/layerhq/Atlas-Android/blob/44b32948858a6ee3c9842eff8c899a88ac7f8945/layer-atlas/src/main/java/com/layer/atlas/messagetypes/threepartimage/ThreePartImageUtils.java#L62
      
### Receiving a GIF

Once the GIF is sent, you have to get the GIF and handle it on the receiving side.
			
* Since GIF is usually greater than 2kb, you need to ensure we download the data by default, by adding it to the default download policy. Since GIF needs to play in UI, you might have set this to download all the message parts for GIF. 

* Eg of how we do this for ThreePartImage preview is here : https://github.com/layerhq/Atlas-Android-Messenger/blob/80e15193bc21281ab69e0c2244ccbd61f9c87741/app/src/main/java/com/layer/messenger/App.java#L148

* Note that it doesn't download the original image data. In GIF, I assume you will add the 3rd part as well.

* Create and add ThreePartGifCellFactory to `AtlasConversationRecyclerView` and `AtlasMessagesRecyclerView`

* Example : https://github.com/layerhq/Atlas-Android-Messenger/blob/80e15193bc21281ab69e0c2244ccbd61f9c87741/app/src/main/java/com/layer/messenger/MessagesListActivity.java#L165

* Code for ThreePartImageCellFactory : https://github.com/layerhq/Atlas-Android/blob/44b32948858a6ee3c9842eff8c899a88ac7f8945/layer-atlas/src/main/java/com/layer/atlas/messagetypes/threepartimage/ThreePartImageCellFactory.java

* Ensure ThreePartGifCellFactory matches correctly for isBindable, and provides the data in bindCellHolder methods. In bindCellHolder you need to decide what you do when the data is not downloaded. You can monitor progress of each message part individually using LayerProgressListener : https://docs.layer.com/sdk/android/richcontent#monitoring-transfer-progress
			
### NOTE:

There are also few other options to consider. If they were not considered, please do consider them before making your decision.

* Don't send the actual gif through layer, but download it from your server as needed. In this case you can just send a unique gif id as message part. This would allow you to download the correct format for the device on which you are rendering

* Convert the gif to video (to reduce size, and compatibility among devices). A discussion of what Twitter does : https://blog.embed.ly/what-twitter-isnt-telling-you-about-gifs-e1b74068cebd#.uq5zj2gi8
