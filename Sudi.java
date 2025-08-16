import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Takes a sequence of words and produce the corresponding sequence of tags using POS tagging via HMM.
 **/

public class Sudi {
    // Observation map: Map<POS, Map<word, frequency>>
    private Map<String, Map<String, Double>> observationMap = new HashMap<>();
    // Transition map: Map<POS, Map<NextPOS, frequency>>
    private Map<String, Map<String, Double>> transitionMap = new HashMap<>();
    private String textFile; // Text file to train with
    private String textFilePOSVar; // Text file's respective POS tags to train with
    // empty constructor; used for test case 0
    public Sudi(){}
    public Sudi(String textFileWords, String textFilePOS) throws IOException {

        try {
            textFile = textFileWords;
            textFilePOSVar = textFilePOS;
            trainSudi(textFile, textFilePOSVar);
        }
        catch (IOException exception) {
            System.out.println("Sorry, this does not work. Here is the error: " + exception.getMessage());
        }
    }
    /**
     * trains Sudi, our text recognition bot, using the text file of words and the text file of POS states for each word
     * @param textFileWords the text file containing the actual sentences and words
     * @param textFilePOS the text file containing the POS states
     * @throws IOException
     */
    public void trainSudi(String textFileWords, String textFilePOS) throws IOException {

        try {
            BufferedReader textFileWordsBuff = new BufferedReader(new FileReader(textFileWords));
            BufferedReader textFilePOSToReadOne = new BufferedReader(new FileReader(textFilePOS));
            // ---------------------------------------------------------------------------------
            // create the observation map: Map<POS, Map<Word, frequency>>
            String lineOfWords = textFileWordsBuff.readLine().toLowerCase();
            String lineOfPOSOne = textFilePOSToReadOne.readLine().toLowerCase();

            while (lineOfWords != null && lineOfPOSOne != null) {
                String[] splitLineWords = lineOfWords.split(" ");
                String[] splitLinePOS = lineOfPOSOne.split(" ");
                for (int i = 0; i < splitLineWords.length; i++) {
                    // if map already contains the POS
                    if (observationMap.containsKey(splitLinePOS[i])) {
                        // if the nested map already has a Key for the Word
                        if (observationMap.get(splitLinePOS[i]).containsKey(splitLineWords[i])) {
                            Double tempValue = observationMap.get(splitLinePOS[i]).get(splitLineWords[i]) + 1.0;
                            observationMap.get(splitLinePOS[i]).put(splitLineWords[i], tempValue);
                        }
                        // if the nested map does not have a Key for the POS yet,
                        // put the POS as the Key and 1.0 as the value in the nested map
                        else {
                            observationMap.get(splitLinePOS[i]).put(splitLineWords[i], 1.0);
                        }
                    }
                    // if the map does not already contain the word
                    else {
                        // create the nested Map with the POS as the Key and 1.0 as the value
                        Map<String, Double> tempMap = new HashMap<>();
                        tempMap.put(splitLineWords[i], 1.0);
                        observationMap.put(splitLinePOS[i], tempMap);
                    }
                }

                // create the transition map: Map<POS, Map<NextPOS, frequency>>
                String startingPOS = "#";
                for (int i = 0; i < splitLinePOS.length; i++) {
                    // if the transition map already has a Key for the POS
                    if (transitionMap.containsKey(startingPOS)) {
                        // if the nested map already has a Key for the other POS
                        if (transitionMap.get(startingPOS).containsKey(splitLinePOS[i])) {
                            Double tempValue = transitionMap.get(startingPOS).get(splitLinePOS[i]) + 1.0;
                            transitionMap.get(startingPOS).put(splitLinePOS[i], tempValue);
                        }
                        // if the nested map does not have a Key for the other POS yet,
                        // put the other POS as the Key and 1.0 as the value in the nested map
                        else {
                            transitionMap.get(startingPOS).put(splitLinePOS[i], 1.0);
                        }
                    }
                    // if the map does not already contain the startingPOS
                    else {
                        // create the nested Map with the current POS as the Key and 1.0 as the value
                        Map<String, Double> tempMap = new HashMap<>();
                        tempMap.put(splitLinePOS[i], 1.0);
                        transitionMap.put(startingPOS, tempMap);
                    }
                    // make the new startingPOS as the current POS we are on
                    startingPOS = splitLinePOS[i];
                }
                lineOfWords = textFileWordsBuff.readLine();
                lineOfPOSOne = textFilePOSToReadOne.readLine();
            }
            textFileWordsBuff.close();
            textFilePOSToReadOne.close();

            // ---------------------------------------------------------------------------------

            // normalizing each observation state's counts to probabilities
            for (String i : observationMap.keySet()) {
                double sum = 0;
                for (String i2 : observationMap.get(i).keySet()) {
                    sum += observationMap.get(i).get(i2);
                }
                for (String i2 : observationMap.get(i).keySet()) {
                    observationMap.get(i).put(i2, Math.log(observationMap.get(i).get(i2) / sum));
                }
            }

            // ---------------------------------------------------------------------------------

            // normalizing each transition state's counts to probabilities
            for (String i : transitionMap.keySet()) {
                double sum = 0;
                for (String i2 : transitionMap.get(i).keySet()) {
                    sum += transitionMap.get(i).get(i2);
                }
                for (String i2 : transitionMap.get(i).keySet()) {
                    transitionMap.get(i).put(i2, Math.log(transitionMap.get(i).get(i2) / sum));
                }
            }
        }
        catch (IOException exception) {
            System.out.println("Sorry, this does not work. Here is the error: " + exception.getMessage());
        }
    }

    /**
     * goes through every sentence in the sentencesFile and then calls the actual viterbi
     * algorithm (viterbiHelper) to execute the function
     * @param sentencesFile the text file of sentences
     * @return Array list of sequence of tags for a line as found through viterbi decoding, for every line in file
     * @throws IOException
     */
    public ArrayList viterbi(String sentencesFile) throws IOException {
        ArrayList<ArrayList<String>> returnList = new ArrayList<>();

        try {
            BufferedReader sentences = new BufferedReader(new FileReader(sentencesFile));

            String sentenceLine = sentences.readLine();
            while (sentenceLine != null) {
                sentenceLine.toLowerCase();
                String[] splitLine = sentenceLine.split(" ");
                returnList.add(viterbiHelper(splitLine));
                sentenceLine = sentences.readLine();
            }
        }
        catch (IOException exception){
            System.out.println("Sorry, this does not work. Here is the error: " + exception.getMessage());
        }
        return returnList;
    }

    /**
     * executes the actual viterbi algorithm using current states, current scores, backtracking, next states, next scores, etc.
     * @param sentence an Array String that contains words
     * @return Array list of sequence of tags for a line as found through viterbi decoding
     */

    public ArrayList<String> viterbiHelper(String[] sentence) {

        // create the currStates and currScores HashSet and HashMap
        HashSet<String> currStates = new HashSet<>();
        HashMap<String, Double> currScores = new HashMap<>();

        // create the backtrack list of Maps and the backtrackReturnList of Strings
        ArrayList<Map<String, String>> backtrack = new ArrayList<>();
        ArrayList<String> backtrackReturnList = new ArrayList<>();

        // create the map that will hold the last scores of the viterbi algorithm
        HashMap<String, Double> finalNextScores = new HashMap<>();

        // initialize currStates and currScores with #
        currStates.add("#");
        currScores.put("#", 0.0);

        // ---------------------------------------------------------------------------------
        // iterate through the entire sentence

        for (int i = 0; i < sentence.length; i++) {
            // create the backtrackInnerMap that will then be added to the backtrack arrayList
            HashMap<String, String> backtrackInnerMap = new HashMap<>();
            backtrack.add(backtrackInnerMap);

            // create the nextStates and nextScores HashSet and HashMap
            HashSet<String> nextStates = new HashSet<>();
            HashMap<String, Double> nextScores = new HashMap<>();

            // for each currState in currStates
            for (String currState : currStates) {

                // for each nextState in the transition map for the current state
                if (transitionMap.get(currState) != null) {
                    for (String nextState : transitionMap.get(currState).keySet()) {

                        // add nextState to the nextStates Hash Set
                        nextStates.add(nextState);
                        // initialize the observation score
                        double observationScore;

                        // get the observation score depending on whether or not the word has been observed
                        // in the specific state in the observation map
                        if (observationMap.get(nextState).get(sentence[i]) != null) {
                            observationScore = observationMap.get(nextState).get(sentence[i]);
                        } else {
                            observationScore = -100.0;
                        }

                        // create the nextScore
                        double nextScore = currScores.get(currState) + transitionMap.get(currState).get(nextState) + observationScore;
                        // if there is no score for the state or if nextScore is bigger than the current score in that state
                        if (!nextScores.containsKey(nextState) || nextScore > nextScores.get(nextState)) {
                            nextScores.put(nextState, nextScore);
                            backtrack.get(i).put(nextState, currState);
                        }
                    }
                }
            }
            currStates = nextStates;
            currScores = nextScores;
            finalNextScores = nextScores;
        }

        // ---------------------------------------------------------------------------------

        // find the largest number in the final map of scores
        // we will use this for backtracking purposes
        double maxNum = Double.NEGATIVE_INFINITY;
        for (String i : finalNextScores.keySet()) {
            if (finalNextScores.get(i) > maxNum) {
                maxNum = finalNextScores.get(i);
            }
        }

        // ---------------------------------------------------------------------------------

        int backtrackIndex = backtrack.size()-1;
        String lastPOS = "";

        // use the maxNum to add the last, greatest POS to the backtrackReturnList
        for (String i3 : backtrack.get(backtrackIndex).keySet()) {
            if (finalNextScores.get(i3) == maxNum){
                lastPOS = i3;
                backtrackReturnList.add(lastPOS);
                break;
            }
        }

        // start at the last map of POS's
        backtrackIndex = backtrack.size() - 1;
        // use the previous lastPOS to get the currentPOS (newLastPOS) to add to the backtrackReturnList
        while (backtrackIndex > 0) {
            String newLastPOS = backtrack.get(backtrackIndex).get(lastPOS);
            if (!Objects.equals(lastPOS, "#")) {
                backtrackReturnList.add(0, newLastPOS);
            }
            lastPOS = newLastPOS;
            backtrackIndex--;
        }
        return backtrackReturnList;
    }

    /**
     * hardcodes parts of speech model from PD_HMM.pdf (drill 7)
     */
    public void test0(){
        observationMap = new HashMap<>();
        transitionMap = new HashMap<>();
        // hardcode observation map
        observationMap.put("NP", new HashMap<>());
        observationMap.get("NP").put("chase", Math.log(10));
        observationMap.put("CNJ", new HashMap<>());
        observationMap.get("CNJ").put("and", Math.log(10));
        observationMap.put("V", new HashMap<>());
        observationMap.get("V").put("chase", Math.log(3));
        observationMap.get("V").put("get", Math.log(1));
        observationMap.get("V").put("watch", Math.log(6));
        observationMap.put("N", new HashMap<>());
        observationMap.get("N").put("cat", Math.log(4));
        observationMap.get("N").put("dog", Math.log(4));
        observationMap.get("N").put("watch", Math.log(2));
        // hardcode transition map
        transitionMap.put("#", new HashMap<>());
        transitionMap.get("#").put("NP", Math.log(3));
        transitionMap.get("#").put("N", Math.log(7));
        transitionMap.put("NP", new HashMap<>());
        transitionMap.get("NP").put("V", Math.log(8));
        transitionMap.get("NP").put("CNJ", Math.log(2));
        transitionMap.put("CNJ", new HashMap<>());
        transitionMap.get("CNJ").put("NP", Math.log(2));
        transitionMap.get("CNJ").put("V", Math.log(4));
        transitionMap.get("CNJ").put("N", Math.log(4));
        transitionMap.put("V", new HashMap<>());
        transitionMap.get("V").put("NP", Math.log(4));
        transitionMap.get("V").put("CNJ", Math.log(2));
        transitionMap.get("V").put("N", Math.log(4));
        transitionMap.put("N", new HashMap<>());
        transitionMap.get("N").put("CNJ", Math.log(2));
        transitionMap.get("N").put("V", Math.log(8));
    }

    /**
     * allows for user to input a line to test for its POS tags
     */
    public void consoleTest() throws Exception {

        try {
            Scanner in = new Scanner(System.in);
            System.out.println("Please input a sentence. Enter 'q' at any time to quit.");
            String line = in.nextLine().toLowerCase(); // takes in input from scanner
            while (!line.equals("q")) {
                String[] sentence = line.split(" ");
                System.out.println(viterbiHelper(sentence) + "\n");
                line = in.nextLine().toLowerCase(); // takes in input from scanner
            }
            in.close();
        }
        catch (Exception exception) {
            System.out.println("Sorry, this does not work. Here is the error: " + exception.getMessage());
        }
    }
    /**
     * evaluates the performance on a pair of test files by comparing calculated POS tags to actual POS tags
     * @param sentencesFile source for file that will be read/tested on
     * @param tagsFile source for file that contains actual tags for sentencesFile
     * @param findByLine determines whether or not user wants to receive line-by-line accuracy comments; beneficial
     *                   to keep on with a smaller file, but takes up time and space with larger file
     * @return String of correct tag predictions out of total tags in file, expressed both as a percentage and an
     *                   unsimplified fraction
     * @throws IOException
     */

    public String findAccuracy(String sentencesFile, String tagsFile, boolean findByLine) throws IOException {

        try {
            int correct = 0; // keeps track of correct tag predictions in entire file
            int total = 0; // keeps track of total tags in entire file
            // ArrayList of ArrayLists of calculated tags for each line in file
            ArrayList<ArrayList<String>> calculatedTagsFile = viterbi(sentencesFile);
            BufferedReader tagsReader = new BufferedReader(new FileReader(tagsFile));
            String tagLine = tagsReader.readLine(); // get line from real tags file
            int incrementor = 0; // incrementor for ArrayList of ArrayLists so it is known which ArrayList is being used
            while (tagLine != null && incrementor < calculatedTagsFile.size()) {
                int lineCorrect = 0; // keeps track of correct tag predictions in line
                int lineTotal = 0; // keeps track of total tags in line
                tagLine.toLowerCase(); // should already be lowercase, but just in case
                String[] splitTagLine = tagLine.split(" "); // creates array of actual tags for each line
                for (int i = 0; i < Math.min(calculatedTagsFile.get(incrementor).size(), splitTagLine.length); i++) {
                    if (calculatedTagsFile.get(incrementor).get(i).equals(splitTagLine[i])) {
                        lineCorrect++;
                    }
                    lineTotal++;
                }
                if (findByLine) {
                    System.out.println(lineCorrect + " correct tags out of " + lineTotal + " tags in this line, " +
                            "for a percentage of " + ((double) lineCorrect / lineTotal) * 100 + "%");
                }
                correct += lineCorrect;
                total += lineTotal;
                incrementor++;
                tagLine = tagsReader.readLine(); // read next line from actual tags file
            }
            return correct + " correct tags out of " + total + " tags in this file, for a percentage of " +
                    ((double) correct / total) * 100 + "%\n";
        }
        catch (IOException exception) {
            System.out.println("Sorry, this does not work. Here is the error: " + exception.getMessage());
        }

        return "";

    }
    public static void main(String[] args) throws Exception {

        // All Brown corpus-related files
        String sentencesTestBrown = "texts/brown-test-sentences.txt";
        String tagsTestBrown = "texts/brown-test-tags.txt";
        String sentencesTrainBrown = "texts/brown-train-sentences.txt";
        String tagsTrainBrown = "texts/brown-train-tags.txt";

        // All "example" files
        String sentencesExample = "texts/example-sentences.txt";
        String tagsExample = "texts/example-tags.txt";

        // All "simple" files
        String sentencesTestSimple = "texts/simple-test-sentences.txt";
        String tagsTestSimple = "texts/simple-test-tags.txt";
        String sentencesTrainSimple = "texts/simple-train-sentences.txt";
        String tagsTrainSimple = "texts/simple-train-tags.txt";

        // Test hardcoded case from drill
        Sudi testCase0 = new Sudi();
        testCase0.test0();
        String[] drillArray1 = new String [] {"dog", "and", "cat", "chase"};
        String[] drillArray2 = new String [] {"chase", "watch", "cat"};
        String[] drillArray3 = new String [] {"watch", "get", "chase", "and", "dog"};
        // Since these hard coded sentences do not come from a file, must format them as a String array. Although
        // accuracy method is not run for this test case, a manual check will show that the algorithm guesses the POS
        // tag with complete accuracy.
        System.out.println(testCase0.viterbiHelper(drillArray1));
        System.out.println(testCase0.viterbiHelper(drillArray2));
        System.out.println(testCase0.viterbiHelper(drillArray3));

        // "Example" files test; trains from the same file that it reads from, and thus will score well on accuracy.
        // Test case only used to show that methods work.
        Sudi test1 = new Sudi(sentencesExample, tagsExample);
        System.out.println("\n"+test1.viterbi(sentencesExample));
        System.out.println("\n"+test1.findAccuracy(sentencesExample, tagsExample, true));

        // Brown corpus test
        Sudi test2 = new Sudi(sentencesTrainBrown, tagsTrainBrown);
        System.out.println("\n"+test2.viterbi(sentencesTestBrown));
        System.out.println("\n"+test2.findAccuracy(sentencesTestBrown, tagsTestBrown, false));

        // "Simple" files test
        Sudi test3 = new Sudi(sentencesTrainSimple, tagsTrainSimple);
        System.out.println(test3.viterbi(sentencesTestSimple));
        System.out.println(test3.findAccuracy(sentencesTestSimple, tagsTestSimple, true));
        
        // Console test
        Sudi consoleInput = new Sudi(sentencesTrainBrown, tagsTrainBrown);
        consoleInput.consoleTest();
    }
}