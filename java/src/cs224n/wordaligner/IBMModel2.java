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
public class IBMModel2 implements WordAligner {

  private static final long serialVersionUID = 1315751943476440515L;
  private CounterMap<String,String> parallelCounts;
  private Counter<String> targetWordCounts;
  private CounterMap<String, String> conditionalAlignmentCounts;
  private Counter<String> alignmentCounts;
  private CounterMap<String, String> tCounter;
  private CounterMap<String, String> qCounter;
  private HashSet<String> allTargets = new HashSet<String>();
  private HashSet<String> allSources = new HashSet<String>();
  private int longestSource = 0;
  private int longestTarget = 0;


  public Alignment align(SentencePair sentencePair) {
    Alignment alignment = new Alignment();
    List<String> targetWords = sentencePair.getTargetWords();
    List<String> sourceWords = sentencePair.getSourceWords();
    int targetIndex = 0;
    int m = sourceWords.size();
    int l = targetWords.size();
    int j = -1;
    for (String target : targetWords) {
      j++;
      double currentBestScore = qCounter.getCount("#-1,"+l+","+m+"#", "#"+j+"#")*tCounter.getCount("#NULL#",target);
      int bestIndex = -1;
      int currentIndex = 0;
      int i = -1;
      for (String source : sourceWords) {
        i++;
        double score = qCounter.getCount("#"+i+","+l+","+m+"#", "#"+j+"#")*tCounter.getCount(source,target);
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
    trainT(trainingPairs);
    for(int iter = 0; iter < 50; iter++)
    {
      System.out.println(iter);
      parallelCounts = new CounterMap<String,String>();
      targetWordCounts = new Counter<String>();
      conditionalAlignmentCounts = new CounterMap<String,String>();
      alignmentCounts = new Counter<String>();
      for(SentencePair pair : trainingPairs){
        List<String> targetWords = pair.getTargetWords();
        List<String> sourceWords = pair.getSourceWords();
        int i = -1;
        for(String source : sourceWords){
          i++;
          double deltDenom = 0;
          int j = -1;
          for(String target : targetWords){
            j++;
            deltDenom += qCounter.getCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", "#"+j+"#")*tCounter.getCount(source, target); 
          }
          j = -1;
          for(String target : targetWords){
            j++;
            double delta = (qCounter.getCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", "#"+j+"#")*tCounter.getCount(source, target))/deltDenom;
            parallelCounts.incrementCount(source, target, delta);
            if(i == 0) targetWordCounts.incrementCount(target, delta);
            conditionalAlignmentCounts.incrementCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", "#"+j+"#", delta);
            alignmentCounts.incrementCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", delta);
          }
        }
        String source = "#NULL#";
        i = -1;
        double deltDenom = 0;
          int j = -1;
          for(String target : targetWords){
            j++;
            deltDenom += qCounter.getCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", "#"+j+"#")*tCounter.getCount(source, target); 
          }
          j = -1;
          for(String target : targetWords){
            j++;
            double delta = (qCounter.getCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", "#"+j+"#")*tCounter.getCount(source, target))/deltDenom;
            parallelCounts.incrementCount(source, target, delta);
            conditionalAlignmentCounts.incrementCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", "#"+j+"#", delta);
            alignmentCounts.incrementCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", delta);
          }
      }
      // parallelCounts = Counters.conditionalNormalize(parallelCounts);
      // targetWordCounts = Counters.normalize(targetWordCounts);
      double difference = 0;
      int numCounts = 0;
      for(String source : allSources){
        for(String target : allTargets){
          numCounts++;
          double val = parallelCounts.getCount(source, target)/targetWordCounts.getCount(target);
          double oldVal = tCounter.getCount(source,target);
          difference += Math.abs(val - oldVal);
          if(val > 0 || tCounter.getCount(source, target) != 0) tCounter.setCount(source, target, val);
        }
      }
      for(int m = 0; m <= longestSource; m++){
        for(int l = 0; l <= longestTarget; l++){
          for(int i = -1; i <= m; i++){
            for(int j = 0; j <= l; j++){
              numCounts++;
              double oldVal = tCounter.getCount("#"+i+","+l+","+m+"#", "#"+j+"#");
              double val = conditionalAlignmentCounts.getCount("#"+i+","+l+","+m+"#", "#"+j+"#")/alignmentCounts.getCount("#"+i+","+l+","+m+"#");
              difference += Math.abs(val - oldVal);
              if(val > 0 || qCounter.getCount("#"+i+","+l+","+m+"#", "#"+j+"#") != 0) qCounter.setCount("#"+i+","+l+","+m+"#", "#"+j+"#", val);
            }
          }
        }
      }
      //tCounter = Counters.conditionalNormalize(tCounter);
      if(difference/numCounts <= 0.001) {
        return;
      }
    }
  }

  public void trainT(List<SentencePair> trainingPairs) {
    initializeQT(trainingPairs);
    for(int iter = 0; iter < 50; iter++)
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
      // parallelCounts = Counters.conditionalNormalize(parallelCounts);
      // targetWordCounts = Counters.normalize(targetWordCounts);
      double difference = 0;
      int numCounts = 0;
      for(String source : allSources){
        for(String target : allTargets){
          numCounts++;
          double val = parallelCounts.getCount(source, target)/targetWordCounts.getCount(target);

          double oldVal = tCounter.getCount(source,target);
          difference += Math.abs(val - oldVal);
          if(val > 0 || tCounter.getCount(source, target) != 0) tCounter.setCount(source, target, val);
        }
      }

      //tCounter = Counters.conditionalNormalize(tCounter);
      if(difference/numCounts <= 0.0001) {
        return;
      }
    }
  }

  public void initializeQT(List<SentencePair> trainingPairs){
    tCounter = new CounterMap<String,String>();
    qCounter = new CounterMap<String,String>();
    for(SentencePair pair : trainingPairs){
      List<String> targetWords = pair.getTargetWords();
      List<String> sourceWords = pair.getSourceWords();
      longestSource = Math.max(sourceWords.size(), longestSource);
      longestTarget = Math.max(targetWords.size(), longestTarget);

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

      // for(String source : sourceWords){
      //   allSources.add(source);
      // }
      // allSources.add("#NULL#");
      // for(String target : targetWords){
      //   allTargets.add(target);
      // }
    }

    for(int m = 0; m <= longestSource; m++){
      for(int l = 0; l <= longestTarget; l++){
        for(int i = -1; i <= m; i++){
          for(int j = 0; j <= l; j++){
            qCounter.setCount("#"+i+","+l+","+m+"#", "#"+j+"#", 1.0);
          }
        }
      }
    }
    // for(String source : allSources){
    //   for(String target : allTargets){
    //     tCounter.setCount(source, target, 1.0);
    //   }
    // }
    // tCounter = Counters.conditionalNormalize(tCounter);
  }
}
