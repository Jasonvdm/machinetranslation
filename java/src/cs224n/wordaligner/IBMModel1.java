package cs224n.wordaligner;  

import cs224n.util.*;
import java.util.*;

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
public class IBMModel1 implements WordAligner {

  private static final long serialVersionUID = 1315751943476440515L;
  private CounterMap<String, String> tCounter;
  private HashSet<String> allTargets = new HashSet<String>();
  private HashSet<String> allSources = new HashSet<String>();


  public Alignment align(SentencePair sentencePair) {
    Alignment alignment = new Alignment();
    List<String> targetWords = sentencePair.getTargetWords();
    List<String> sourceWords = sentencePair.getSourceWords();
    int targetIndex = 0;
    for (String target : targetWords) {
      double currentBestScore = tCounter.getCount("#NULL#",target);
      int bestIndex = -1;
      int currentIndex = 0;
      for (String source : sourceWords) {
        double score = tCounter.getCount(source,target);
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
    initializeT(trainingPairs);
    for(int iter = 0; iter < 15; iter++)
    {
      System.out.println(iter);
      CounterMap<String, String> newTCounter = new CounterMap<String,String>();
      for(SentencePair pair : trainingPairs){
        List<String> targetWords = pair.getTargetWords();
        List<String> sourceWords = pair.getSourceWords();
        for(String target : targetWords){
          double deltDenom = 0;
          for(String source : sourceWords){
            deltDenom += tCounter.getCount(source, target); 
          }
          for(String source : sourceWords){
            double delta = tCounter.getCount(source, target)/deltDenom;
            newTCounter.incrementCount(source, target, delta);
          }
        }
        String source = "#NULL#";
        double deltDenom = 0;
        for(String target : targetWords){
          deltDenom += tCounter.getCount(source, target); 
        }
        for(String target : targetWords){
          double delta = tCounter.getCount(source, target)/deltDenom;
          newTCounter.incrementCount(source, target, delta);
        }
      }
      newTCounter = Counters.conditionalNormalize(newTCounter);
      tCounter = newTCounter;
    }
  }

  public void initializeT(List<SentencePair> trainingPairs){
    tCounter = new CounterMap<String,String>();
    int iterations = 0;
    for(SentencePair pair : trainingPairs){
      if(iterations%1000 == 0) System.out.println("Training sentence: "+iterations);
      iterations ++;
      List<String> targetWords = pair.getTargetWords();
      List<String> sourceWords = pair.getSourceWords();

      allSources.add("#NULL#");
      for (String source : sourceWords) {
        for (String target : targetWords) {
          allSources.add(source);
          allTargets.add(target);
          tCounter.setCount(source, target, 1.0);
        }
      }

      for (String target : allTargets) {
        tCounter.setCount("#NULL#", target, 1.0);
      }
    }
    tCounter = Counters.conditionalNormalize(tCounter);
  }
}
