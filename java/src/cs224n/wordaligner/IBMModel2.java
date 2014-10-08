package cs224n.wordaligner;  

import cs224n.util.*;
import java.util.*;
import java.lang.*;

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
  private HashSet<Integer> sourceLengths = new HashSet<Integer>();
  private HashSet<Integer> targetLengths = new HashSet<Integer>();


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
      double currentBestScore = Math.log(qCounter.getCount("#-1,"+l+","+m+"#", "#"+j+"#")) + Math.log(tCounter.getCount("#NULL#",target));
      int bestIndex = -1;
      int currentIndex = 0;
      int i = -1;
      for (String source : sourceWords) {
        i++;
        double score = Math.log(qCounter.getCount("#"+i+","+l+","+m+"#", "#"+j+"#"))+Math.log(tCounter.getCount(source,target));
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
      CounterMap<String, String> newTCounter = new CounterMap<String,String>();
      CounterMap<String, String> newQCounter = new CounterMap<String,String>();
      for(SentencePair pair : trainingPairs){
        List<String> targetWords = pair.getTargetWords();
        List<String> sourceWords = pair.getSourceWords();
        int j = -1;
        for(String target : targetWords){
          j++;
          double deltDenom = 0;
          int i = -1;
          for(String source : sourceWords){
            i++;
            deltDenom += qCounter.getCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", "#"+j+"#")*tCounter.getCount(source, target); 
          }
          i = -1;
          for(String source : sourceWords){
            i++;
            double delta = (qCounter.getCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", "#"+j+"#")*tCounter.getCount(source, target))/deltDenom;
            if(Double.isNaN(delta)) delta = 0;
            newTCounter.incrementCount(source, target, delta);
            newQCounter.incrementCount("#"+i+","+targetWords.size()+","+sourceWords.size()+"#", "#"+j+"#", delta);
          }
        }
        String source = "#NULL#";
        int h = -1;
        double deltDenom = 0;
          int k = -1;
          for(String target : targetWords){
            k++;
            deltDenom += qCounter.getCount("#"+k+","+targetWords.size()+","+sourceWords.size()+"#", "#"+h+"#")*tCounter.getCount(source, target); 
          }
          k = -1;
          for(String target : targetWords){
            k++;
            double delta = (qCounter.getCount("#"+k+","+targetWords.size()+","+sourceWords.size()+"#", "#"+h+"#")*tCounter.getCount(source, target))/deltDenom;
            if(Double.isNaN(delta)) delta = 0;
            newTCounter.incrementCount(source, target, delta);
            newQCounter.incrementCount("#"+k+","+targetWords.size()+","+sourceWords.size()+"#", "#"+h+"#", delta);
          }
      }

      tCounter = newTCounter;
      tCounter = Counters.conditionalNormalize(tCounter);
      qCounter = newQCounter;
      qCounter = Counters.conditionalNormalize(qCounter);
    }
  }

  public void trainT(List<SentencePair> trainingPairs) {
    initializeQT(trainingPairs);
    for(int iter = 0; iter < 50; iter++)
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

  public void initializeQT(List<SentencePair> trainingPairs){
    tCounter = new CounterMap<String,String>();
    for(SentencePair pair : trainingPairs){
      List<String> targetWords = pair.getTargetWords();
      List<String> sourceWords = pair.getSourceWords();

      targetLengths.add(targetWords.size());
      sourceLengths.add(sourceWords.size());

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

    qCounter = new CounterMap<String,String>();
    for(int m : targetLengths){
      for(int l : sourceLengths){
        for(int i = 0; i < m; i++){
          for(int j = -1; j < l; j++){
            qCounter.setCount("#"+i+","+l+","+m+"#", "#"+j+"#", Math.random());
          }
        }
      }
    }
    tCounter = Counters.conditionalNormalize(tCounter);
    qCounter = Counters.conditionalNormalize(qCounter);
  }
}
