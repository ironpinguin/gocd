/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.configrepo.contract.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.contract.AbstractCRTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CRGitMaterialTest extends AbstractCRTest<CRGitMaterial> {
    private String url1 = "http://my.git.repository.com";
    private String url2 = "http://other.git.repository.com";

    private final CRGitMaterial simpleGit;
    private final CRGitMaterial simpleGitBranch;
    private final CRGitMaterial veryCustomGit;
    private final CRGitMaterial invalidNoUrl;
    private final CRGitMaterial withIncludes;
    private final CRGitMaterial invalidBothIncludesAndIgnores;
    private final CRGitMaterial invalidPasswordAndEncryptedPasswordSet;

    public CRGitMaterialTest() {
        simpleGit = new CRGitMaterial();
        simpleGit.setUrl(url1);

        simpleGitBranch = new CRGitMaterial();
        simpleGitBranch.setUrl(url2);
        simpleGitBranch.setBranch("develop");

        veryCustomGit = new CRGitMaterial("gitMaterial1", "dir1", false, false, null, List.of("externals", "tools"), url1, "feature12", true);
        withIncludes = new CRGitMaterial("gitMaterial1", "dir1", false, true, null, List.of("externals", "tools"), url1, "feature12", true);

        invalidNoUrl = new CRGitMaterial("gitMaterial1", "dir1", false, false, null, List.of("externals", "tools"), null, "feature12", true);
        invalidBothIncludesAndIgnores = new CRGitMaterial("gitMaterial1", "dir1", false, false, null, List.of("externals", "tools"), url1, "feature12", true);
        invalidBothIncludesAndIgnores.setIncludesNoCheck("src", "tests");

        invalidPasswordAndEncryptedPasswordSet = new CRGitMaterial("gitMaterial1", "dir1", false, false, null, List.of("externals", "tools"), null, "feature12", true);
        invalidPasswordAndEncryptedPasswordSet.setPassword("pa$sw0rd");
        invalidPasswordAndEncryptedPasswordSet.setEncryptedPassword("26t=$j64");
    }

    @Override
    public void addGoodExamples(Map<String, CRGitMaterial> examples) {
        examples.put("simpleGit", simpleGit);
        examples.put("simpleGitBranch", simpleGitBranch);
        examples.put("veryCustomGit", veryCustomGit);
        examples.put("withIncludes", withIncludes);
    }

    @Override
    public void addBadExamples(Map<String, CRGitMaterial> examples) {
        examples.put("invalidNoUrl", invalidNoUrl);
        examples.put("invalidBothIncludesAndIgnores", invalidBothIncludesAndIgnores);
    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials() {
        JsonObject jsonObject = (JsonObject) gson.toJsonTree(veryCustomGit);
        assertThat(jsonObject.get("type").getAsString()).isEqualTo(CRGitMaterial.TYPE_NAME);
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializing() {
        CRMaterial value = veryCustomGit;
        String json = gson.toJson(value);

        CRGitMaterial deserializedValue = (CRGitMaterial) gson.fromJson(json, CRMaterial.class);
        assertThat(deserializedValue).isEqualTo(value);
    }

}
