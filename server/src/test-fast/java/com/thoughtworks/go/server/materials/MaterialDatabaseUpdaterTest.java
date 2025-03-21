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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialExpansionService;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MaterialDatabaseUpdaterTest {
    @Mock private MaterialRepository materialRepository;
    @Mock private ServerHealthService healthService;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private DependencyMaterialUpdater dependencyMaterialUpdater;
    @Mock private ScmMaterialUpdater scmMaterialUpdater;
    @Mock private PackageMaterialUpdater packageMaterialUpdater;
    @Mock private PluggableSCMMaterialUpdater pluggableSCMMaterialUpdater;
    @Mock private MaterialExpansionService materialExpansionService;
    @Mock private GoConfigService goConfigService;

    private MaterialDatabaseUpdater materialDatabaseUpdater;

    @BeforeEach
    public void setUp() {
        materialDatabaseUpdater = new MaterialDatabaseUpdater(materialRepository, healthService, transactionTemplate, dependencyMaterialUpdater, scmMaterialUpdater,
                packageMaterialUpdater, pluggableSCMMaterialUpdater, materialExpansionService, goConfigService);
    }

    @Test
    public void shouldThrowExceptionWithLongDescriptionOfMaterialWhenUpdateFails() {
        Material material = new GitMaterial("url", "branch");
        Exception exception = new RuntimeException("failed");
        String message = "Modification check failed for material: " + material.getLongDescription() + "\nAffected pipelines are blah.";
        when(goConfigService.pipelinesWithMaterial(material.config().getFingerprint())).thenReturn(List.of(new CaseInsensitiveString("blah")));
        ServerHealthState error = ServerHealthState.errorWithHtml(message, exception.getMessage(), HealthStateType.general(HealthStateScope.forMaterial(material)));
        when(materialRepository.findMaterialInstance(material)).thenThrow(exception);
        try {
            materialDatabaseUpdater.updateMaterial(material);
            fail("should have thrown exception");
        } catch (Exception e) {
            assertThat(e).isEqualTo(exception);
        }
        verify(healthService).update(error);
    }

    @Test
    public void shouldGetCorrectUpdaterForMaterials() {
        assertThat(materialDatabaseUpdater.updater(MaterialsMother.dependencyMaterial())).isEqualTo(dependencyMaterialUpdater);
        assertThat(materialDatabaseUpdater.updater(MaterialsMother.svnMaterial())).isEqualTo(scmMaterialUpdater);
        assertThat(materialDatabaseUpdater.updater(MaterialsMother.packageMaterial())).isEqualTo(packageMaterialUpdater);
        assertThat(materialDatabaseUpdater.updater(MaterialsMother.pluggableSCMMaterial())).isEqualTo(pluggableSCMMaterialUpdater);
    }

    @Test
    public void shouldFailWithAReasonableMessageWhenExceptionMessageIsNull() {
        Material material = new GitMaterial("url", "branch");
        Exception exceptionWithNullMessage = new RuntimeException(null, new RuntimeException("Inner exception has non-null message"));
        String message = "Modification check failed for material: " + material.getLongDescription() + "\nNo pipelines affected, may only affect configuration repositories.";
        when(goConfigService.pipelinesWithMaterial(material.config().getFingerprint())).thenReturn(Collections.emptyList());

        when(materialRepository.findMaterialInstance(material)).thenThrow(exceptionWithNullMessage);

        try {
            materialDatabaseUpdater.updateMaterial(material);
            fail("should have thrown exception");
        } catch (Exception e) {
            assertThat(e).isEqualTo(exceptionWithNullMessage);
        }

        verify(healthService).update(ServerHealthState.errorWithHtml(message, "Unknown error", HealthStateType.general(HealthStateScope.forMaterial(material))));
    }
}
