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
package com.thoughtworks.go.util.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PasswordArgumentTest {
    private final CommandArgument argument = new PasswordArgument("secret");

    @Test
    public void shouldReturnStringValueForCommandLine() {
        assertThat(argument.originalArgument()).isEqualTo("secret");
    }

    @Test
    public void shouldReturnStringValueForReporting() {
        assertThat(argument.forDisplay()).isEqualTo("******");
    }

    @Test
    public void shouldReturnValueForToString() {
        assertThat(argument.toString()).isEqualTo("******");
    }

    @Test
    public void shouldReturnSampNumberOfStarsForAnyPassword() {
        assertThat(new PasswordArgument("foo").toString()).isEqualTo("******");
        assertThat(new PasswordArgument("very very long password").toString()).isEqualTo("******");
    }

}
