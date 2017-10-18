package com.layer.atlas.test;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.layer.atlas.messagetypes.generic.GenericCellFactory;
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
public class GenericCellFactoryTest {
    private final static String MIME_TYPE = "text/plain";

    private List<MessagePart> mTestMessageParts;
    private MessagePart mTextMessagePart;
    private Message mMessage;
    private LayerClient mLayerClient;

    @Before
    public void setUp() {
        mTestMessageParts = new ArrayList<>();
        mTextMessagePart = new MockMessagePart("Generic message".getBytes(), MIME_TYPE);
        mTestMessageParts.add(mTextMessagePart);
        mMessage = new MockMessageImpl(mTestMessageParts);
        mLayerClient = new MockLayerClient();
    }

    @Test
    public void testParseContent() {
        GenericCellFactory genericCellFactory = new GenericCellFactory();
        GenericCellFactory.ParsedContent parsedContent = genericCellFactory.parseContent(mLayerClient, mMessage);

        assertThat(parsedContent.getString().isEmpty(), is(false));
    }

    @Test
    public void testIsType() {
        GenericCellFactory genericCellFactory = new GenericCellFactory();

        assertThat(genericCellFactory.isType(mMessage), is(true));
    }

    @Test
    public void testIsBindable() {
        GenericCellFactory genericCellFactory = new GenericCellFactory();

        assertThat(genericCellFactory.isBindable(mMessage), is(true));
    }

    @Test
    public void testGetPreviewText() {
        GenericCellFactory genericCellFactory = new GenericCellFactory();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertThat(genericCellFactory.getPreviewText(context, mMessage).isEmpty(), is(false));
    }
}
