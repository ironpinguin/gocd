/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv6.shared.representers.stages;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.apiv6.shared.representers.EnvironmentVariableRepresenter;
import com.thoughtworks.go.config.*;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.util.HashMap;
import java.util.function.Consumer;

public class StageRepresenter {

  public static void toJSON(OutputWriter jsonWriter, StageConfig stageConfig) {
    if (!stageConfig.errors().isEmpty()) {
      jsonWriter.addChild("errors", errorWriter -> {
        new ErrorGetter(new HashMap<>()).toJSON(errorWriter, stageConfig);
      });
    }

    jsonWriter.addIfNotNull("name", stageConfig.name());
    jsonWriter.add("fetch_materials", stageConfig.isFetchMaterials());
    jsonWriter.add("clean_working_directory", stageConfig.isCleanWorkingDir());
    jsonWriter.add("never_cleanup_artifacts", stageConfig.isArtifactCleanupProhibited());
    jsonWriter.addChild("approval", approvalWriter -> ApprovalRepresenter.toJSON(approvalWriter, stageConfig.getApproval()));
    jsonWriter.addChildList("environment_variables", envVarsWriter -> EnvironmentVariableRepresenter.toJSON(envVarsWriter, stageConfig.getVariables()));
    jsonWriter.addChildList("jobs", getJobs(stageConfig));

  }

  private static Consumer<OutputListWriter> getJobs(StageConfig stageConfig) {
    return jobsWriter -> {
      stageConfig.getJobs().forEach(job -> {
        jobsWriter.addChild(jobWriter -> JobRepresenter.toJSON(jobWriter, job));
      });
    };
  }

  public static StageConfig fromJSON(JsonReader jsonReader) {
    StageConfig stageConfig = new StageConfig();
    if (jsonReader == null) {
      return stageConfig;
    }
    jsonReader.readCaseInsensitiveStringIfPresent("name", stageConfig::setName);
    if (jsonReader.hasJsonObject("fetch_materials")) {
      Boolean fetchMaterials = jsonReader.optBoolean("fetch_materials").get();
      stageConfig.setFetchMaterials(fetchMaterials);
    }

    if (jsonReader.hasJsonObject("clean_working_directory")) {
      Boolean cleanWorkingDirectory = jsonReader.optBoolean("clean_working_directory").get();
      stageConfig.setCleanWorkingDir(cleanWorkingDirectory);
    }

    if (jsonReader.hasJsonObject("never_cleanup_artifacts")) {
      Boolean neverCleanupArtifacts = jsonReader.optBoolean("never_cleanup_artifacts").get();
      stageConfig.setArtifactCleanupProhibited(neverCleanupArtifacts);
    }

    setEnvironmentVariables(jsonReader, stageConfig);
    setJobs(jsonReader, stageConfig);
    if (jsonReader.hasJsonObject("approval")) {
      Approval approval = ApprovalRepresenter.fromJSON(jsonReader.readJsonObject("approval"));
      stageConfig.setApproval(approval);
    }
    return stageConfig;
  }

  private static void setEnvironmentVariables(JsonReader jsonReader, StageConfig stageConfig) {
    EnvironmentVariablesConfig environmentVariableConfigs = new EnvironmentVariablesConfig();
    jsonReader.readArrayIfPresent("environment_variables", variables -> {
      variables.forEach(variable -> {
        try {
          environmentVariableConfigs.add(EnvironmentVariableRepresenter.fromJSON(new JsonReader(variable.getAsJsonObject())));
        } catch (InvalidCipherTextException e) {
          e.printStackTrace();
        }
      });
    });
    stageConfig.setVariables(environmentVariableConfigs);
  }

  private static void setJobs(JsonReader jsonReader, StageConfig stageConfig) {
    JobConfigs allJobs = new JobConfigs();
    jsonReader.readArrayIfPresent("jobs", jobs -> {
      jobs.forEach(job -> {
        allJobs.add(JobRepresenter.fromJSON(new JsonReader(job.getAsJsonObject())));
      });
    });

    stageConfig.setJobs(allJobs);
  }
}
