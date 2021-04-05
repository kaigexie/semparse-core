import io.github.clearwsd.parser.*;
import io.github.semlink.verbnet.*;
import io.github.semlink.parser.*;
import io.github.semlink.propbank.type.PropBankArg;
import io.github.semlink.semlink.VerbNetAligner;

import java.io.*;

import static io.github.semlink.parser.VerbNetParser.pbRoleLabeler;

public class VerbNetParserForRoc {

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

        try {
            FileInputStream fstream = new FileInputStream("test.txt");
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            File myObj = new File("test-parsing-output.txt");
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }

            FileWriter myWriter = new FileWriter("test-parsing-output.txt");
            String strLine;
            int i = 1;
            while ((strLine = br.readLine()) != null) {
                System.out.println(i);
                i++;
                VerbNetParse parse = null;
                try {
                    parse = parser.parse(strLine);
                } catch (Exception ee) {

                }
                myWriter.write("**********\n");
                if (parse != null) {
                    myWriter.write(parse.toString());
                } else {
                    myWriter.write("NOPARSINGRESULT\n");
                }
                myWriter.flush();
            }

            in.close();
            myWriter.close();
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

    }

}
