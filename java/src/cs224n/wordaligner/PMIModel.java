package cs224n.wordaligner;  

import cs224n.util.*;
import java.util.List;

/**
 * Simple word alignment baseline model that maps source positions to target 
 * positions along the diagonal of the alignment grid.
 * 
 * IMPORTANT: Make sure that you read the comments in the
 * cs224n.wordaligner.WordAligner interface.
 * 
 * @author Jason van der Merwe
 * @author Bridge Eimon
 */
public class PMIModel implements WordAligner {

  private static final long serialVersionUID = 1315751943476440515L;
  
  // TODO: Use arrays or Counters for collecting sufficient statistics
  // from the training data.
  private CounterMap<String,String> parallelCounts;
  private Counter<String> sourceWordCounts;


  public Alignment align(SentencePair sentencePair) {
    // Placeholder code below. 
    // TODO Implement an inference algorithm for Eq.1 in the assignment
    // handout to predict alignments based on the counts you collected with train().
    Alignment alignment = new Alignment();
    List<String> targetWords = sentencePair.getTargetWords();
    List<String> sourceWords = sentencePair.getSourceWords();
    int targetIndex = 0;
    for (String target : targetWords) {
      double currentBestScore = parallelCounts.getCount("#NULL#", target)/sourceWordCounts.getCount("#NULL#");
      int bestIndex = -1;
      int currentIndex = 0;
      for (String source : sourceWords) {
        double score = parallelCounts.getCount(source, target)/sourceWordCounts.getCount(source);
        if (score > currentBestScore) {
          currentBestScore = score;
          bestIndex = currentIndex;
        }
        currentIndex++;
      }
      if(bestIndex >= 0) alignment.addPredictedAlignment(targetIndex, bestIndex);
      targetIndex++;
    }
    return alignment;
  }

  public void train(List<SentencePair> trainingPairs) {
    parallelCounts = new CounterMap<String,String>();
    sourceWordCounts = new Counter<String>();

    for(SentencePair pair : trainingPairs){
      sourceWordCounts.incrementCount("#NULL#", 1.0);
      List<String> targetWords = pair.getTargetWords();
      List<String> sourceWords = pair.getSourceWords();
      // sourceWords.add("#{{{null}}}");
      for(String source : sourceWords){
        for(String target : targetWords){
          parallelCounts.incrementCount(source, target, 1.0);
        }
        sourceWordCounts.incrementCount(source, 1.0);
      }

      for(String target : targetWords){
          parallelCounts.incrementCount("#NULL#", target, 1.0);
        }
    }
  }
}
