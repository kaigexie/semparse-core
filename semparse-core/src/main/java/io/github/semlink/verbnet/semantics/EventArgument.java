/*
 * Copyright 2019 James Gung
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

package io.github.semlink.verbnet.semantics;

import com.google.common.base.Strings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.semlink.verbnet.type.SemanticArgumentType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import static io.github.semlink.verbnet.semantics.SemanticPredicate.DEFAULT_EVENT;

/**
 * Event argument.
 *
 * @author jgung
 */
@Slf4j
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(of = "id", callSuper = false)
public class EventArgument<T> extends VariableSemanticArgument<T> {

    public static final Pattern EVENT_PATTERN = Pattern.compile(
            "(during|end|start|result)?(\\([EËeë](?<index1>\\d?)\\)|[EËeë](?<index2>\\d?))",
            Pattern.CASE_INSENSITIVE);

    public EventArgument(@NonNull String value) {
        super(SemanticArgumentType.EVENT, value);
        Matcher matcher = EVENT_PATTERN.matcher(value);
        if (matcher.find()) {
            String rel = matcher.group(1);
            if (!Strings.isNullOrEmpty(rel)) {
                relation = EventRelation.valueOf(matcher.group(1).toUpperCase());
            }
            id = matcher.group(2).toUpperCase();
            if (id.startsWith("Ë") || id.startsWith("ë")) {
                process = true;
            }
            if (id.equals(DEFAULT_EVENT) || id.equals("Ë")) {
                mainEvent = true;
            } else {
                if (id.startsWith("Ë")) {
                    id = "ë" + id.substring(1);
                }
                id = id.toLowerCase();
            }
            String index1 = matcher.group("index1");
            String index2 = matcher.group("index2");
            if (!(Strings.isNullOrEmpty(index1) && Strings.isNullOrEmpty(index2))) {
                index = Integer.parseInt(Strings.isNullOrEmpty(index1) ? index2 : index1);
            }
        } else {
            log.warn("Failed to parse event argument: {}", value);
        }
    }

    public enum EventRelation {
        EVENT,
        START,
        DURING,
        END,
        RESULT
    }

    private EventRelation relation = EventRelation.EVENT;
    private String id = DEFAULT_EVENT;
    private boolean mainEvent = false;
    private boolean process = false;
    private int index = 0;

    @Override
    public String toString() {
        return relation.toString() + "(" + id + (variable == null ? "" : " = " + variable.toString()) + ")";
    }
}
