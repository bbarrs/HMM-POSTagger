# HMM-POSTagger
HMM-POSTagger is a part-of-speech (POS) tagging system using a Hidden Markov Model (HMM) with Viterbi decoding.  
Trains on Brown corpus and some other tagged sentence files to learn transition/emission probabilities. Initial state probabilities are estimated via transitions from designated start symbol ("#") to the first tag of each sentence.

Predicts the most likely sequence of POS tags for unseen sentences with ~96.5% accuracy (Brown)

Train/test datasets not provided.
