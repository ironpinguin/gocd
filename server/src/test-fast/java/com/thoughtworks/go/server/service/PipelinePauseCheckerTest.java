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

import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelinePauseCheckerTest {
    private String pipelineName;
    private PipelinePauseChecker checker;
    public PipelinePauseService pipelinePauseService;

    @BeforeEach
    public void setUp() {
        pipelinePauseService = mock(PipelinePauseService.class);
        pipelineName = "cruise";
        checker = new PipelinePauseChecker(pipelineName, pipelinePauseService);
    }

    @Test
    public void shouldReturnFailureResultIfPipelineIsPaused() {
        when(pipelinePauseService.isPaused(pipelineName)).thenReturn(true);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        checker.check(result);
        assertThat(result.getServerHealthState().isSuccess()).isFalse();
        assertThat(result.getServerHealthState().getDescription()).isEqualTo("Pipeline cruise is paused");
    }

    @Test
    public void shouldReturnPassExceptionIfPipelineIsNotPaused() {
        when(pipelinePauseService.isPaused(pipelineName)).thenReturn(false);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        checker.check(result);
        assertThat(result.getServerHealthState().isSuccess()).isTrue();
    }

    @Test
    public void shouldReturnConflict_soHttpUsersCanMakeSenseOfIt() {
        when(pipelinePauseService.isPaused(pipelineName)).thenReturn(true);
        HttpOperationResult result = new HttpOperationResult();
        checker.check(result);
        assertThat(result.httpCode()).isEqualTo(409);
        assertThat(result.getServerHealthState().isSuccess()).isFalse();
        assertThat(result.getServerHealthState().getDescription()).isEqualTo("Pipeline cruise is paused");
    }
}
