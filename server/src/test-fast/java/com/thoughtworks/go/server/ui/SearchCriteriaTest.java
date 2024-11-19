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
package com.thoughtworks.go.server.ui;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchCriteriaTest {

    @Test
    public void shouldReturnTrueIfTheSearchTermMatches() throws Exception {
        SearchCriteria criteria = new SearchCriteria("win");
        assertThat(criteria.matches("windows")).isTrue();
        assertThat(criteria.matches("win")).isTrue();
        criteria = new SearchCriteria("windows");
        assertThat(criteria.matches("windows")).isTrue();
        assertThat(criteria.matches("win")).isFalse();
    }

    @Test
    public void shouldPerformExactMatch() throws Exception {
        SearchCriteria criteria = new SearchCriteria("\"win\"");
        assertThat(criteria.matches("windows")).isFalse();
        assertThat(criteria.matches("win")).isTrue();
    }
    @Test
    public void shouldHandleInvalidInput() {
        SearchCriteria criteria = new SearchCriteria("\"\"\"");
        assertThat(criteria.matches("")).isFalse();
        criteria = new SearchCriteria("\"");
        assertThat(criteria.matches("")).isFalse();

    }


}
