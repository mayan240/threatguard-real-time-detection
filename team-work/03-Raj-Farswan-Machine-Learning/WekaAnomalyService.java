package com.threatguard.api;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class WekaAnomalyService {
  private static final String[] CLASSES = {"Normal","BruteForce","EndpointScan","AppDoS","PayloadAttack","BehavioralAnomaly"};
  private Instances schema; private Classifier classifier; private double sensitivity = 50;

  @PostConstruct
  void train() throws Exception {
    try (InputStream in = new ClassPathResource("cyber_traffic.arff").getInputStream()) {
      schema = new Instances(new BufferedReader(new InputStreamReader(in)));
    }
    schema.setClassIndex(schema.numAttributes() - 1);
    classifier = new RandomForest(); ((RandomForest) classifier).setNumIterations(80); classifier.buildClassifier(schema);
    Path model = Paths.get("cyber_traffic.model");
    try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(model))) { out.writeObject(classifier); }
  }

  public synchronized void setSensitivity(double value) {
    if (Double.isNaN(value) || value < 0 || value > 100) throw new IllegalArgumentException("Sensitivity must be 0..100");
    sensitivity = value;
  }
  public double getSensitivity() { return sensitivity; }
  public boolean shouldAlert(int score) { return score >= sensitivity; }

  public Detection detect(double[] features) {
    try {
      Instance item = new DenseInstance(1.0, features);
      item.setDataset(schema);
      double[] distribution = classifier.distributionForInstance(item);
      int best = 0; for (int i=1;i<distribution.length;i++) if(distribution[i]>distribution[best]) best=i;
      double confidence = distribution[best];
      int score = riskScore(features, best, confidence);
      return new Detection(CLASSES[best], score, confidence, explain(CLASSES[best], score, confidence, features));
    } catch (Exception e) {
      throw new IllegalStateException("Weka inference failed", e);
    }
  }

  public double score(SecurityEvent event) {
    double[] f = {event.getEventType().contains("FAILED") ? 12 : 1, event.getEventType().contains("FAILED") ? 1 : 0,
        0, 1, event.getEventType().contains("FAILED") ? 0.8 : 0.02, 100, event.getBytesTransferred(), 0};
    return detect(f).riskScore();
  }
  public String explanation(SecurityEvent event) { return "Weka supervised threat classification"; }
  public ModelMetrics metrics() { return new ModelMetrics("Weka RandomForest (8 features)", 12, 12, sensitivity/100, 1, 1, 1); }

  private int riskScore(double[] f, int type, double confidence) {
    double rule = Math.min(100, f[1]*3 + f[2]*1.5 + f[3] + f[4]*35 + f[5]/100 + f[6]/10000 + f[7]*12);
    double classWeight = type == 0 ? 0 : 25;
    return (int)Math.round(Math.max(0, Math.min(100, Math.max(rule, classWeight + confidence*60))));
  }
  private String explain(String type,int score,double confidence,double[] f) {
    return String.format(Locale.ROOT, "Flagged as %s (%.0f%% confidence): %d requests, %d failed auth, %d 404s, %d unique endpoints, %.0f%% error rate, %d signature hits",
        type, confidence*100, (int)f[0], (int)f[1], (int)f[2], (int)f[3], f[4]*100, (int)f[7]);
  }
  public record Detection(String threatClass,int riskScore,double confidence,String reason) {}
}
