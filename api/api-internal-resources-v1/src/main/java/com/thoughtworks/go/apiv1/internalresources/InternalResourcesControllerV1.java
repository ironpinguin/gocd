/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.apiv1.internalresources;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.api.base.JsonOutputWriter;
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static spark.Spark.*;

@Component
public class InternalResourcesControllerV1 extends ApiController implements SparkSpringController {
    private final ApiAuthenticationHelper apiAuthenticationHelper;
    private GoConfigService goConfigService;
    private final AgentService agentService;

    @Autowired
    public InternalResourcesControllerV1(ApiAuthenticationHelper apiAuthenticationHelper, GoConfigService goConfigService, AgentService agentService) {
        super(ApiVersion.v1);
        this.apiAuthenticationHelper = apiAuthenticationHelper;
        this.goConfigService = goConfigService;
        this.agentService = agentService;
    }

    @Override
    public String controllerBasePath() {
        return Routes.InternalResources.BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before("", mimeType, this::setContentType);
            before("/*", mimeType, this::setContentType);

            before("", mimeType, this.apiAuthenticationHelper::checkUserAnd403);

            get("", mimeType, this::index);
        });
    }

    public String index(Request request, Response response) throws IOException {
        List<String> resourceListFromGoConfig = goConfigService.getResourceList();
        List<String> resourceListFromAgentDB = agentService.getListOfResourcesAcrossAgents();
        resourceListFromGoConfig.addAll(resourceListFromAgentDB);
        List<String> finalResourceList = resourceListFromGoConfig.stream().distinct().collect(toList());
        return JsonOutputWriter.OBJECT_MAPPER.writeValueAsString(finalResourceList);
    }
}
