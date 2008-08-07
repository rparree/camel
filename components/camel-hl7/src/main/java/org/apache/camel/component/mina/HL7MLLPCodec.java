/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.mina;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.camel.dataformat.hl7.HL7DataFormat;
import org.apache.camel.dataformat.hl7.HL7Converter;

import ca.uhn.hl7v2.model.Message;

/**
 * HL7 MLLP codec.
 * <p/>
 * This codec supports encoding/decoding the HL7 MLLP protocol.
 * It will use the default markers for start and end combination:
 * <ul>
 *   <li>0x0b (11 decimal) = start marker</li>
 *   <li>0x0d (13 decimal = the \r char) = segment terminators</li>
 *   <li>0x1c (28 decimal) = end 1 marker</li>
 *   <li>0x0d (13 decimal) = end 2 marker</li>
 * </ul>
 * <p/>
 * The decoder is used for decoding from MLLP (bytes) to String. The String will not contain any of
 * the start and end markers.
 * <p/>
 * The encoder is used for encoding from String to MLLP (bytes). The String should <b>not</b> contain
 * any of the start and end markers, the enoder will add these, and stream the string as bytes.
 * Also the enocder will convert any <tt>\n</tt> (line breaks) as segment terminators to <tt>\r</tt>.
 * <p/>
 * This codes supports charset encoding/decoding between bytes and String. The JVM platform default charset
 * is used, but the charset can be configued on this codec using the setter method.
 * The decoder will use the JVM platform default charset for decoding, but the charset can be configued on the this codec.
 */
public class HL7MLLPCodec implements ProtocolCodecFactory {

    private static final String CHARSET_ENCODER = HL7MLLPCodec.class.getName() + ".charsetencoder";
    private static final String CHARSET_DECODER = HL7MLLPCodec.class.getName() + ".charsetdecoder";

    // HL7 MLLP start and end markers
    private static final byte START_MARKER = 0x0b; // 11 decimal
    private static final byte END_MARKER_1 = 0x1c; // 28 decimal
    private static final byte END_MARKER_2 = 0x0d; // 13 decimal

    private Charset charset = Charset.defaultCharset();

    public ProtocolEncoder getEncoder() throws Exception {
        return new ProtocolEncoder() {
            public void encode(IoSession session, Object message, ProtocolEncoderOutput out)
                throws Exception {
                
                if (message == null) {
                    throw new IllegalArgumentException("Message to encode is null");
                }

                CharsetEncoder encoder = (CharsetEncoder)session.getAttribute(CHARSET_ENCODER);
                if (encoder == null) {
                    encoder = charset.newEncoder();
                    session.setAttribute(CHARSET_ENCODER, encoder);
                }

                // convert to string
                String body;
                if (message instanceof byte[]) {
                    // body is most likely a byte[]
                    body = new String((byte[])message, encoder.charset().name());
                } else if (message instanceof Message) {
                    // but can also be a HL7 Message
                    body = HL7Converter.toString((Message)message);
                } else {
                    // fallback to the toString method
                    body = message.toString();
                }
                // replace \n with \r as HL7 uses 0x0d = \r as segment termninators
                body = body.replace('\n', '\r');

                // put the data into the byte buffer
                ByteBuffer bb = ByteBuffer.allocate(body.length() + 3).setAutoExpand(true);
                bb.put(START_MARKER);
                bb.putString(body, encoder);
                bb.put(END_MARKER_1);
                bb.put(END_MARKER_2);

                // flip the buffer so we can use it to write to the out stream
                bb.flip();
                out.write(bb);
            }

            public void dispose(IoSession session) throws Exception {
                session.removeAttribute(CHARSET_ENCODER);
            }
        };
    }

    public ProtocolDecoder getDecoder() throws Exception {
        return new ProtocolDecoder() {
            public void decode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {

                // find position where we have the end1 end2 combination
                int posEnd = 0;
                int posStart = 0;
                while (in.hasRemaining()) {
                    byte b = in.get();
                    if (b == START_MARKER) {
                        posStart = in.position();
                    }
                    if (b == END_MARKER_1) {
                        byte next = in.get();
                        if (next == END_MARKER_2) {
                            posEnd = in.position();
                            break;
                        }
                    }
                }

                // okay we have computed the start and end position of the special HL7 markers
                // rewind the bytebuffer so we can read from it again
                in.rewind();

                // narrow the buffer to only include between the start and end markers
                in.skip(posStart);
                if (posEnd > 0) {
                    in.limit(posEnd);
                }

                try {
                    // convert to string using the charset decoder
                    CharsetDecoder decoder = (CharsetDecoder)session.getAttribute(CHARSET_DECODER);
                    if (decoder == null) {
                        decoder = charset.newDecoder();
                        session.setAttribute(CHARSET_DECODER, decoder);
                    }
                    String body = in.getString(decoder);

                    out.write(body);
                } finally {
                    // clear the buffer now that we have transfered the data to the String
                    in.clear();
                }
            }

            public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
                // do nothing
            }

            public void dispose(IoSession session) throws Exception {
                session.removeAttribute(CHARSET_DECODER);
            }
        };
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public void setCharset(String charsetName) {
        this.charset = Charset.forName(charsetName);
    }

}
