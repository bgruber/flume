/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flume.interceptor;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.flume.Context;
import org.apache.flume.Event;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Created by bgruber on 9/18/15.
 */
public class AvroToJSInterceptor implements Interceptor {
    BinaryDecoder decoder = null;
    GenericRecord data = null;

    LoadingCache<String, Schema> schemaCache = CacheBuilder.newBuilder().build(new CacheLoader<String, Schema>() {
        @Override
        public Schema load(String s) throws Exception {
            return new Schema.Parser().parse(new URL(s).openStream());
        }
    });


    @Override
    public void initialize() {}

    @Override
    public Event intercept(Event event) {
        String schemaUrl = event.getHeaders().get("flume.avro.schema.url");
        Schema s = schemaCache.getUnchecked(schemaUrl);
        decoder = DecoderFactory.get().binaryDecoder(
                event.getBody(),
                decoder
        );
        try {
            data = new GenericDatumReader<GenericRecord>(s).read(data, decoder);
            String json = GenericData.get().toString(data);
            event.setBody(json.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return event;
    }

    @Override
    public List<Event> intercept(List<Event> events) {
        for (Event event : events) {
            intercept(event);
        }
        return events;
    }

    @Override
    public void close() {}

    public static class Builder implements Interceptor.Builder {
        @Override
        public Interceptor build() {
            return new AvroToJSInterceptor();
        }

        @Override
        public void configure(Context context) {}
    }

}
