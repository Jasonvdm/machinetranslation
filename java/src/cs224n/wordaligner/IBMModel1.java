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
  private CounterMap<String,String> parallelCounts;
  private Counter<String> targetWordCounts;
  private CounterMap<String, String> conditionalAlignmentCounts;
  private Counter<String> alignmentCounts;
  private CounterMap<String, String> tCounter;
  private ArrayList<String> allTargets = new ArrayList<String>();
  private ArrayList<String> allSources = new ArrayList<String>();


  public Alignment align(SentencePair sentencePair) {
    System.out.println("Starting aligning now");
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
    System.out.println("Ending aligning now");
    return alignment;
  }

  public void train(List<SentencePair> trainingPairs) {
    initializeT(trainingPairs);
    System.out.println("Starting training now");


    for(int iter = 0; iter < 1000; iter++)
    {
      System.out.println(iter);
      parallelCounts = new CounterMap<String,String>();
      targetWordCounts = new Counter<String>();
      conditionalAlignmentCounts = new CounterMap<String,String>();
      alignmentCounts = new Counter<String>();
      for(SentencePair pair : trainingPairs){
        List<String> targetWords = pair.getTargetWords();
        List<String> sourceWords = pair.getSourceWords();
        // sourceWords.add("#{{{null}}}");
        int i = -1;
        for(String source : sourceWords){
          i++;
          double deltDenom = 0;
          for(String target : targetWords){
            deltDenom += tCounter.getCount(source, target); 
          }
          int j = -1;
          for(String target : targetWords){
            j++;
            double delta = tCounter.getCount(source, target)/deltDenom;
            parallelCounts.incrementCount(source, target, delta);
            if(i == 0) targetWordCounts.incrementCount(target, delta);
            //conditionalAlignmentCounts.incrementCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", "#"+j+"#", delta);
            //alignmentCounts.incrementCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", delta);
          }
        }
        String source = "#NULL#";
        double deltDenom = 0;
        for(String target : targetWords){
          deltDenom += tCounter.getCount(source, target); 
        }
        int j = -1;
        for(String target : targetWords){
          j++;
          double delta = tCounter.getCount(source, target)/deltDenom;
          parallelCounts.incrementCount(source, target, delta);
          //conditionalAlignmentCounts.incrementCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", "#"+j+"#", delta);
          //alignmentCounts.incrementCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", delta);
        }
      }
      //Counters.conditionalNormalize(parallelCounts);
      //Counters.normalize(targetWordCounts);
      double difference = 0;
      int numCounts = 0;
      System.out.println("Updating TCounter");
      for(String source : allSources){
        for(String target : allTargets){
          numCounts++;
          double val = parallelCounts.getCount(source, target)/targetWordCounts.getCount(target);
          double oldVal = tCounter.getCount(source,target);
          difference += Math.abs(val - oldVal);
          tCounter.setCount(source, target, val);
        }
      }
      //Counters.conditionalNormalize(tCounter);
      if(difference/numCounts <= 0.0001) {
        System.out.println("Ending training now");
        return;
      }
    }
    System.out.println("Ending training now");
  }

  public void initializeT(List<SentencePair> trainingPairs){
    tCounter = new CounterMap<String,String>();
    for(SentencePair pair : trainingPairs){
      List<String> targetWords = pair.getTargetWords();
      List<String> sourceWords = pair.getSourceWords();
      for(String source : sourceWords){
        allSources.add(source);
      }
      allSources.add("#NULL#");
      for(String target : targetWords){
        allTargets.add(target);
      }
    }
    for(String source : allSources){
      for(String target : allTargets){
        tCounter.setCount(source, target, 1.0);
      }
    }
    Counters.conditionalNormalize(tCounter);
  }
}
