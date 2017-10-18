package com.layer.atlas.test;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.layer.atlas.messagetypes.text.TextCellFactory;
import com.layer.atlas.mock.MockLayerClient;
import com.layer.atlas.mock.MockMessageImpl;
import com.layer.atlas.mock.MockMessagePart;
import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Message;
import com.layer.sdk.messaging.MessagePart;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class TextCellFactoryTest {
    private final static String MIME_TYPE = "text/plain";
    private static final String TEST_MESSAGE_TEXT = "Hello, world!";

    private List<MessagePart> mTestMessageParts;
    private MessagePart mTextMessagePart;
    private Message mMessage;
    private LayerClient mLayerClient;

    @Before
    public void setUp() {
        mTestMessageParts = new ArrayList<>();
        mTextMessagePart = new MockMessagePart(TEST_MESSAGE_TEXT.getBytes(), MIME_TYPE);
        mTestMessageParts.add(mTextMessagePart);
        mMessage = new MockMessageImpl(mTestMessageParts);
        mLayerClient = new MockLayerClient();
    }

    @Test
    public void testParseContent() {
        TextCellFactory textCellFactory = new TextCellFactory();
        TextCellFactory.TextInfo parsedContent = textCellFactory.parseContent(mLayerClient, mMessage);

        assertThat(parsedContent.getString().isEmpty(), is(false));
        assertThat(parsedContent.getString(), is(TEST_MESSAGE_TEXT));
    }

    @Test
    public void testIsType() {
        TextCellFactory textCellFactory = new TextCellFactory();

        assertThat(textCellFactory.isType(mMessage), is(true));
    }

    @Test
    public void testIsBindable() {
        TextCellFactory textCellFactory = new TextCellFactory();
        assertThat(textCellFactory.isBindable(mMessage), is(true));
    }

    @Test
    public void testGetPreviewText() {
        TextCellFactory textCellFactory = new TextCellFactory();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertThat(textCellFactory.getPreviewText(context, mMessage).isEmpty(), is(false));
        assertThat(textCellFactory.getPreviewText(context, mMessage), is(TEST_MESSAGE_TEXT));
    }
}
