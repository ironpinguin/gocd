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
package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.StageIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CcTrayStageStatusLoaderTest {
    @Mock
    private StageDao stageDao;
    @Mock
    private CcTrayStageStatusChangeHandler stageChangeHandler;

    private CcTrayStageStatusLoader loader;

    @BeforeEach
    public void setUp() {
        loader = new CcTrayStageStatusLoader(stageDao, stageChangeHandler);
    }

    @Test
    public void shouldNotHaveAnyStatusesIfAStageCannotBeFoundInDB() {
        setupStagesInDB(new StageIdentity("pipeline1", "stage1", 12L), new StageIdentity("pipeline2", "stage2", 14L));

        List<ProjectStatus> actualStatuses = loader.getStatusesForStageAndJobsOf(pipelineConfigFor("pipeline1"), stageConfigFor("non-existent-stage"));

        assertThat(actualStatuses).isEqualTo(Collections.<ProjectStatus>emptyList());
    }

    @Test
    public void shouldConvertToStatusesIfAStageIsFoundInDB() {
        List<ProjectStatus> expectedStatuses = List.of(new ProjectStatus("pipeline1 :: stage1", "Sleeping", "some-status", "some-label", new Date(), "some-url"));
        List<Stage> stages = setupStagesInDB(new StageIdentity("pipeline1", "stage1", 12L), new StageIdentity("pipeline2", "stage2", 14L));
        when(stageChangeHandler.statusesOfStageAndItsJobsFor(stages.get(0))).thenReturn(expectedStatuses);

        List<ProjectStatus> actualStatuses = loader.getStatusesForStageAndJobsOf(pipelineConfigFor("pipeline1"), stageConfigFor("stage1"));

        assertThat(actualStatuses).isEqualTo(expectedStatuses);
    }

    @Test
    public void shouldCacheResultOfLatestStageInstancesOnce() {
        setupStagesInDB(new StageIdentity("pipeline1", "stage1", 12L), new StageIdentity("pipeline2", "stage2", 14L));

        loader.getStatusesForStageAndJobsOf(pipelineConfigFor("pipeline1"), stageConfigFor("stage1"));
        loader.getStatusesForStageAndJobsOf(pipelineConfigFor("pipeline1"), stageConfigFor("stage2"));
        loader.getStatusesForStageAndJobsOf(pipelineConfigFor("pipeline1"), stageConfigFor("stage-some-nonexistent-one"));

        verify(stageDao, times(1)).findLatestStageInstances();
    }

    private List<Stage> setupStagesInDB(StageIdentity... stageIdentities) {
        when(stageDao.findLatestStageInstances()).thenReturn(Arrays.asList(stageIdentities));

        List<Stage> stages = new ArrayList<>();
        for (StageIdentity identity : stageIdentities) {
            Stage stage = StageMother.custom(identity.getPipelineName() + " - " + identity.getStageName());
            lenient().when(stageDao.stageById(identity.getStageId())).thenReturn(stage);
            stages.add(stage);
        }

        return stages;
    }

    private PipelineConfig pipelineConfigFor(String pipelineName) {
        return PipelineConfigMother.pipelineConfig(pipelineName);
    }

    private StageConfig stageConfigFor(String stageName) {
        return StageConfigMother.custom(stageName);
    }
}
