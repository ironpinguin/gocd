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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class StageOrderTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private StageDao stageDao;
    @Autowired private PipelineService pipelineService;
    @Autowired private ScheduleService scheduleService;
	@Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private PipelineWithTwoStages preCondition;
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        preCondition = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);

        dbHelper.onSetUp();
        preCondition.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @AfterEach
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        preCondition.onTearDown();
    }

    @Test
    public void shouldSetOrderTo1ForUnScheduledFirstStage() {
        pipelineService.save(preCondition.schedulePipeline());
        Pipeline pipeline = pipelineService.mostRecentFullPipelineByName(preCondition.pipelineName);
        assertThat(pipeline.getStages().first().getOrderId()).isEqualTo(1);
    }

    @Test
    public void shouldIncreaseOrderBy1ForUnScheduledNextStage() {
        schedulePipelineWithFirstStage();

        Pipeline pipeline = pipelineService.mostRecentFullPipelineByName(preCondition.pipelineName);
        dbHelper.passStage(pipeline.getFirstStage());

        scheduleService.automaticallyTriggerRelevantStagesFollowingCompletionOf(pipeline.getFirstStage());

        Pipeline mostRecent = pipelineService.mostRecentFullPipelineByName(preCondition.pipelineName);
        assertThat(mostRecent.getStages().byName(preCondition.ftStage).getOrderId()).isEqualTo(1001);
    }

    @Test
    public void shouldKeepOrderForScheduledStage() {
        schedulePipelineWithFirstStage();

        Pipeline pipeline = pipelineService.mostRecentFullPipelineByName(preCondition.pipelineName);
        dbHelper.passStage(pipeline.getFirstStage());

        Pipeline mostRecent = pipelineService.mostRecentFullPipelineByName(preCondition.pipelineName);
        scheduleService.rerunStage(mostRecent, preCondition.devStage(), "anyone");

        mostRecent = pipelineService.mostRecentFullPipelineByName(preCondition.pipelineName);
        assertThat(mostRecent.getFirstStage().getOrderId()).isEqualTo(1000);
    }

    private void schedulePipelineWithFirstStage() {
        Pipeline pipeline = preCondition.schedulePipeline();
        pipelineDao.save(pipeline);
        pipeline.getFirstStage().setOrderId(1000);
        stageDao.saveWithJobs(pipeline, pipeline.getFirstStage());
    }
}



