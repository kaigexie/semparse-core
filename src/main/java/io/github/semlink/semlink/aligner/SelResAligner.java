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

package io.github.semlink.semlink.aligner;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultiset;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.clearwsd.type.DepNode;
import io.github.clearwsd.type.FeatureType;
import io.github.semlink.propbank.type.ArgNumber;
import io.github.semlink.propbank.type.FunctionTag;
import io.github.semlink.semlink.PropBankPhrase;
import io.github.semlink.verbnet.type.FramePhrase;
import io.github.semlink.verbnet.type.PrepType;
import io.github.semlink.verbnet.type.ThematicRoleType;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import static io.github.semlink.semlink.aligner.AlignmentUtils.getPrep;
import static io.github.semlink.verbnet.type.ThematicRoleType.AGENT;
import static io.github.semlink.verbnet.type.ThematicRoleType.ASSET;
import static io.github.semlink.verbnet.type.ThematicRoleType.ATTRIBUTE;
import static io.github.semlink.verbnet.type.ThematicRoleType.BENEFICIARY;
import static io.github.semlink.verbnet.type.ThematicRoleType.CO_AGENT;
import static io.github.semlink.verbnet.type.ThematicRoleType.CO_PATIENT;
import static io.github.semlink.verbnet.type.ThematicRoleType.CO_THEME;
import static io.github.semlink.verbnet.type.ThematicRoleType.DESTINATION;
import static io.github.semlink.verbnet.type.ThematicRoleType.DIRECTION;
import static io.github.semlink.verbnet.type.ThematicRoleType.EXTENT;
import static io.github.semlink.verbnet.type.ThematicRoleType.GOAL;
import static io.github.semlink.verbnet.type.ThematicRoleType.INITIAL_LOCATION;
import static io.github.semlink.verbnet.type.ThematicRoleType.INITIAL_STATE;
import static io.github.semlink.verbnet.type.ThematicRoleType.INSTRUMENT;
import static io.github.semlink.verbnet.type.ThematicRoleType.LOCATION;
import static io.github.semlink.verbnet.type.ThematicRoleType.MANNER;
import static io.github.semlink.verbnet.type.ThematicRoleType.MATERIAL;
import static io.github.semlink.verbnet.type.ThematicRoleType.PATH;
import static io.github.semlink.verbnet.type.ThematicRoleType.PREDICATE;
import static io.github.semlink.verbnet.type.ThematicRoleType.PRODUCT;
import static io.github.semlink.verbnet.type.ThematicRoleType.RECIPIENT;
import static io.github.semlink.verbnet.type.ThematicRoleType.RESULT;
import static io.github.semlink.verbnet.type.ThematicRoleType.SOURCE;
import static io.github.semlink.verbnet.type.ThematicRoleType.STIMULUS;
import static io.github.semlink.verbnet.type.ThematicRoleType.TIME;
import static io.github.semlink.verbnet.type.ThematicRoleType.TOPIC;
import static io.github.semlink.verbnet.type.ThematicRoleType.TRAJECTORY;

/**
 * Aligner based on selectional restrictions.
 *
 * @author jgung
 */
@AllArgsConstructor
public class SelResAligner implements PbVnAligner {

    private Function<PropBankPhrase, Multiset<ThematicRoleType>> roleMapper;

    public SelResAligner() {
        this(SelResAligner::getThematicRolesStrict);
    }

    @Override
    public void align(@NonNull PbVnAlignment alignment) {
        for (PropBankPhrase phrase : alignment.sourcePhrases(false)) {

            Multiset<ThematicRoleType> thematicRoles = roleMapper.apply(phrase);

            List<ThematicRoleType> sorted = thematicRoles.entrySet().stream()
                    .sorted(Ordering.natural().reverse().onResultOf(Multiset.Entry::getCount))
                    .map(Multiset.Entry::getElement).collect(Collectors.toList());

            for (ThematicRoleType type : sorted) {
                Optional<FramePhrase> framePhrase = alignment.byRole(type);
                if (framePhrase.isPresent()) {
                    if (!alignment.alignedTarget(framePhrase.get())) {
                        alignment.add(phrase, framePhrase.get());
                        break;
                    }
                }
            }
        }

        // apply restrictions
        Optional<FramePhrase> framePhrase = alignment.byRole(ASSET);
        if (framePhrase.isPresent()) {
            PropBankPhrase source = alignment.getSource(framePhrase.get());
            if (null != source) {
                if (!containsNumber(source)) {
                    alignment.remove(source, framePhrase.get());
                }
            }
        }
    }

    public static Multiset<ThematicRoleType> getThematicRolesGreedy(@NonNull PropBankPhrase phrase) {
        Multiset<ThematicRoleType> themRoles = getThematicRolesStrict(phrase);
        Optional<PrepType> prep = getPrep(phrase.tokens());

        boolean possibleLocation = !ImmutableSet.of(FunctionTag.PRP, FunctionTag.MNR).contains(phrase.argument().getFunctionTag());
        if (prep.isPresent()) {
            PrepType type = prep.get();
            if (possibleLocation && type.maybeDestination()) {
                themRoles.add(DESTINATION);
                themRoles.add(GOAL);
                themRoles.add(RECIPIENT);
            }
            if (type == PrepType.TO) {
                themRoles.add(PRODUCT);
                themRoles.add(RESULT);
                themRoles.add(PREDICATE);
            }
            if (possibleLocation && type.maybeLocation() && phrase.argument().getFunctionTag() != FunctionTag.TMP) {
                themRoles.add(LOCATION);
            }
            if (possibleLocation && (type.isTrajectory() || type == PrepType.FROM)) {
                themRoles.add(DIRECTION);
                themRoles.add(TRAJECTORY);
            }
        }
        return themRoles;
    }


    public static Multiset<ThematicRoleType> getThematicRolesStrict(@NonNull PropBankPhrase phrase) {
        Multiset<ThematicRoleType> themRoles = TreeMultiset.create();
        Optional<PrepType> prep = getPrep(phrase.tokens());

        String text = phrase.tokens().stream()
                .map(token -> token.feature(FeatureType.Text).toString())
                .collect(Collectors.joining(" "))
                .toLowerCase();

        if (text.equalsIgnoreCase("how much") || text.equalsIgnoreCase("how much money")) {
            themRoles.add(ASSET);
        }

        boolean isClause = AlignmentUtils.isClause(phrase.tokens());

        // preposition heuristics
        if (prep.isPresent()) {
            PrepType type = prep.get();
            if (type == PrepType.TO) {
                if (isClause) {
                    themRoles.add(TOPIC);
                } else {
                    if (phrase.argument().getFunctionTag() != FunctionTag.PRP) {
                        themRoles.add(DESTINATION);
                    }
                    themRoles.add(BENEFICIARY);
                    themRoles.add(RECIPIENT);
                }
                themRoles.add(RESULT);
                themRoles.add(GOAL);
            } else if (type == PrepType.TOWARDS) {
                themRoles.add(DESTINATION);
            } else if (type.maybeSource()) {
                themRoles.add(INITIAL_LOCATION);
                themRoles.add(INITIAL_STATE);
                themRoles.add(SOURCE);
                themRoles.add(MATERIAL);
            } else if (type == PrepType.INTO) {
                themRoles.add(PRODUCT);
                themRoles.add(RESULT);
                themRoles.add(PREDICATE);
            } else if (type == PrepType.ONTO) {
                themRoles.add(DESTINATION);
            } else if (type == PrepType.FOR) {
                if (containsNumber(phrase)) {
                    themRoles.add(ASSET);
                } else {
                    themRoles.add(BENEFICIARY);
                    themRoles.add(GOAL);
                    themRoles.add(RECIPIENT);
                }
            } else if (type == PrepType.AS) {
                themRoles.add(ATTRIBUTE);
            } else if (type == PrepType.ABOUT) {
                themRoles.add(TOPIC);
                themRoles.add(STIMULUS);
            } else if (type == PrepType.WITH) {
                themRoles.add(CO_AGENT);
                themRoles.add(CO_PATIENT);
                themRoles.add(CO_THEME);
                themRoles.add(INSTRUMENT);
            } else if (type == PrepType.OUT_OF) {
                themRoles.add(MATERIAL);
            } else if (type == PrepType.BY) {
                if (containsNumber(phrase)) {
                    themRoles.add(EXTENT);
                }
            } else if (type.isTrajectory()) {
                themRoles.add(TRAJECTORY);
            }
        }

        if (containsNumber(phrase)) {
            themRoles.add(ASSET);
        }

        if (startsWithWhere(phrase)) {
            if (phrase.getFunctionTag() == FunctionTag.GOL) {
                themRoles.add(DESTINATION);
            } else if (phrase.getFunctionTag() == FunctionTag.DIR) {
                themRoles.add(INITIAL_LOCATION);
                themRoles.add(SOURCE);
            } else {
                themRoles.add(DESTINATION);
                themRoles.add(INITIAL_LOCATION);
                themRoles.add(SOURCE);
            }
            themRoles.add(LOCATION);
        }

        if (phrase.getFunctionTag() == FunctionTag.TMP) {
            themRoles.add(TIME);
        } else if (phrase.getFunctionTag() == FunctionTag.DIR) {
            if (prep.map(p -> p != PrepType.FROM).orElse(true)) {
                themRoles.add(DESTINATION);
            }
            themRoles.add(PATH);
            themRoles.add(TRAJECTORY);
            themRoles.add(DIRECTION);
            themRoles.add(SOURCE);
        }

        if (phrase.getNumber() == ArgNumber.A0) {
            themRoles.add(AGENT);
        }

        if (phrase.getFunctionTag() == FunctionTag.MNR) {
            themRoles.add(MANNER);
        }

        return themRoles;
    }

    public static boolean containsNumber(PropBankPhrase phrase) {
        for (DepNode node : phrase.tokens()) {
            if ("CD".equalsIgnoreCase(node.feature(FeatureType.Pos)) || "much".equalsIgnoreCase(node.feature(FeatureType.Text))) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWithWhere(PropBankPhrase phrase) {
        return getFirstLemma(phrase).equalsIgnoreCase("where");
    }

    private static String getFirstLemma(PropBankPhrase phrase) {
        return phrase.start().feature(FeatureType.Lemma);
    }

}
