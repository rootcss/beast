package com.gojek.beast.consumer;

import com.gojek.beast.converter.ConsumerRecordConverter;
import com.gojek.beast.models.ParseException;
import com.gojek.beast.models.Record;
import com.gojek.beast.models.Records;
import com.gojek.beast.models.Status;
import com.gojek.beast.models.SuccessStatus;
import com.gojek.beast.sink.Sink;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessageConsumerTest {
    @Captor
    private ArgumentCaptor<Records> recordsCaptor;
    @Mock
    private ConsumerRecords messages;
    private long timeout = 10;
    @Mock
    private KafkaConsumer<byte[], byte[]> kafkaConsumer;
    @Mock
    private Sink sink;
    @Mock
    private ConsumerRecordConverter converter;
    @Mock
    private List<Record> records;
    private MessageConsumer consumer;
    private Status success = new SuccessStatus();

    @Before
    public void setUp() {
        consumer = new MessageConsumer(kafkaConsumer, sink, converter, timeout);
        when(kafkaConsumer.poll(timeout)).thenReturn(messages);
    }

    @Test
    public void shouldConsumeMessagesAndPushToSink() throws ParseException {
        when(converter.convert(messages)).thenReturn(records);
        when(sink.push(any())).thenReturn(success);
        InOrder callOrder = inOrder(converter, sink);

        Status status = consumer.consume();

        callOrder.verify(converter).convert(messages);
        callOrder.verify(sink).push(recordsCaptor.capture());
        assertEquals(records, recordsCaptor.getValue().getRecords());
        assertTrue(status.isSuccess());
    }

    @Test
    public void shouldReturnFailureStatusWhenParsingFails() throws ParseException {
        when(converter.convert(any())).thenThrow(new ParseException("test reason", null));
        Status status = consumer.consume();

        assertFalse(status.isSuccess());
    }
}
