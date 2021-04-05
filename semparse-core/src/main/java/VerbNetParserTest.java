import io.github.clearwsd.parser.*;
import io.github.semlink.verbnet.*;
import io.github.semlink.parser.*;
import io.github.semlink.propbank.type.PropBankArg;
import io.github.semlink.semlink.VerbNetAligner;

import static io.github.semlink.parser.VerbNetParser.pbRoleLabeler;

public class VerbNetParserTest {

    public static void main(String[] args) {
        // VerbNet index over VerbNet classes/frames
        VnIndex verbNet = new DefaultVnIndex();

        // Dependency parser used for WSD model and alignment logic
        NlpParser dependencyParser = new Nlp4jDependencyParser();
        // WSD model for predicting VerbNet classes (uses ClearWSD and the NLP4J parser)
        VerbNetSenseClassifier classifier = VerbNetSenseClassifier.fromModelPath("semparse/nlp4j-verbnet-3.3.bin",
                verbNet, dependencyParser);
        // PropBank semantic role labeler from a TF NLP saved model
        SemanticRoleLabeler<PropBankArg> roleLabeler = pbRoleLabeler("semparse/propbank-srl");
        // maps nominal predicates with light verbs to VerbNet classes (e.g. take a bath -> dress-41.1.1)
        LightVerbMapper verbMapper = LightVerbMapper.fromMappingsPath("semparse/lvm.tsv", verbNet);
        // aligner that uses PropBank VerbNet mappings and heuristics to align PropBank roles with VerbNet thematic roles
        VerbNetAligner aligner = VerbNetAligner.of("semparse/pbvn-mappings.json", "semparse/unified-frames.bin");
        VnPredicateDetector predicateDetector = new DefaultVnPredicateDetector(classifier, verbMapper);

        // simplifying facade over the above components
        VerbNetParser parser = new VerbNetParser(predicateDetector, classifier, roleLabeler, aligner);

        VerbNetParse parse = parser.parse("John ate an apple");
        System.out.println(parse); // Take In[EVENT(E1 = VnClassXml(verbNetId=eat-39.1)), Agent(A0[John]), Patient(A1[an apple])]
    }

}