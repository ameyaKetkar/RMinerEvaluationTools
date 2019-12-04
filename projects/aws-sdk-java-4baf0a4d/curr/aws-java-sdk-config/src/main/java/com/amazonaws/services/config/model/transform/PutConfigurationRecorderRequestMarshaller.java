/*
 * Copyright 2010-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * 
 *  http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.services.config.model.transform;

import static com.amazonaws.util.StringUtils.UTF8;
import static com.amazonaws.util.StringUtils.COMMA_SEPARATOR;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;

import com.amazonaws.AmazonClientException;
import com.amazonaws.Request;
import com.amazonaws.DefaultRequest;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.config.model.*;
import com.amazonaws.transform.Marshaller;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.StringUtils;
import com.amazonaws.util.StringInputStream;
import com.amazonaws.util.json.*;

/**
 * Put Configuration Recorder Request Marshaller
 */
public class PutConfigurationRecorderRequestMarshaller implements Marshaller<Request<PutConfigurationRecorderRequest>, PutConfigurationRecorderRequest> {

    public Request<PutConfigurationRecorderRequest> marshall(PutConfigurationRecorderRequest putConfigurationRecorderRequest) {
        if (putConfigurationRecorderRequest == null) {
            throw new AmazonClientException("Invalid argument passed to marshall(...)");
        }

        Request<PutConfigurationRecorderRequest> request = new DefaultRequest<PutConfigurationRecorderRequest>(putConfigurationRecorderRequest, "AmazonConfig");
        String target = "StarlingDoveService.PutConfigurationRecorder";
        request.addHeader("X-Amz-Target", target);

        request.setHttpMethod(HttpMethodName.POST);
        request.setResourcePath("");
        
        try {
          StringWriter stringWriter = new StringWriter();
          JSONWriter jsonWriter = new JSONWriter(stringWriter);

          jsonWriter.object();
          
            ConfigurationRecorder configurationRecorder = putConfigurationRecorderRequest.getConfigurationRecorder();
            if (configurationRecorder != null) {

                jsonWriter.key("ConfigurationRecorder");
                jsonWriter.object();

                if (configurationRecorder.getName() != null) {
                    jsonWriter.key("name").value(configurationRecorder.getName());
                }
                if (configurationRecorder.getRoleARN() != null) {
                    jsonWriter.key("roleARN").value(configurationRecorder.getRoleARN());
                }
                RecordingGroup recordingGroup = configurationRecorder.getRecordingGroup();
                if (recordingGroup != null) {

                    jsonWriter.key("recordingGroup");
                    jsonWriter.object();

                    if (recordingGroup.isAllSupported() != null) {
                        jsonWriter.key("allSupported").value(recordingGroup.isAllSupported());
                    }

                    com.amazonaws.internal.ListWithAutoConstructFlag<String> resourceTypesList = (com.amazonaws.internal.ListWithAutoConstructFlag<String>)(recordingGroup.getResourceTypes());
                    if (resourceTypesList != null && !(resourceTypesList.isAutoConstruct() && resourceTypesList.isEmpty())) {

                        jsonWriter.key("resourceTypes");
                        jsonWriter.array();

                        for (String resourceTypesListValue : resourceTypesList) {
                            if (resourceTypesListValue != null) {
                                jsonWriter.value(resourceTypesListValue);
                            }
                        }
                        jsonWriter.endArray();
                    }
                    jsonWriter.endObject();
                }
                jsonWriter.endObject();
            }

          jsonWriter.endObject();

          String snippet = stringWriter.toString();
          byte[] content = snippet.getBytes(UTF8);
          request.setContent(new StringInputStream(snippet));
          request.addHeader("Content-Length", Integer.toString(content.length));
          request.addHeader("Content-Type", "application/x-amz-json-1.1");
        } catch(Throwable t) {
          throw new AmazonClientException("Unable to marshall request to JSON: " + t.getMessage(), t);
        }

        return request;
    }
}
