/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package trimmer;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import org.biojava3.core.sequence.features.QualityFeature;
import org.biojava3.sequencing.io.fastq.*;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCalculator;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.ui.Layer;

/**
 * Class usefull for trimming fastq reads with different formats (SOLEXA, SANGER
 * and ILLUMINA).
 *
 * @author Giovanni Bacci, Dept. of Evolutionary Biology (DBE), Florence, via
 * Romana 17 - CRA-RPS, Rome, via della Navicella. email:
 * giovanni.bacci@unifi.it
 */
public class StreamingTrimmer {

    private Path inputPath = null;
    private Path outputPath = null;
    private FastqReader fastqReader = null;
    private FastqWriter fastqWriter = null;
    private InputSupplier<InputStreamReader> inputSupplier = null;
    private FastqVariant variant = null;
    final private Map<Integer, Long> qualMap = new TreeMap<>();
    final private Map<Integer, Long> lenMap = new TreeMap<>();
    final private Map<String, Double> stat = new TreeMap<>();
    final private static Charset CS = Charset.forName("UTF-8");

    /**
     * Create a trimmer that stream the content of a fastq file and trim bases
     * with quality above mean quality - standard deviation using a dynamic
     * trimming algorithm
     *
     * @param inputPath path of the input fastq file
     * @param readsType the type of the encoded quality
     * @throws IOException
     */
    public StreamingTrimmer(Path inputPath, String readsType) throws IOException {
        this.inputPath = inputPath;
        switch (readsType.toLowerCase()) {
            case "illumina":
                this.fastqReader = new IlluminaFastqReader();
                this.fastqWriter = new IlluminaFastqWriter();
                this.variant = FastqVariant.FASTQ_ILLUMINA;
                break;
            case "sanger":
                this.fastqReader = new SangerFastqReader();
                this.fastqWriter = new SangerFastqWriter();
                this.variant = FastqVariant.FASTQ_SANGER;
                break;
            case "solexa":
                this.fastqReader = new SolexaFastqReader();
                this.fastqWriter = new SolexaFastqWriter();
                this.variant = FastqVariant.FASTQ_SOLEXA;
                break;
        }
        if (java.nio.file.Files.exists(this.inputPath)) {
            inputSupplier = Files.newReaderSupplier(this.inputPath.toFile(), CS);
        } else {
            throw new IOException("The input file does not exist: "
                    + inputPath.toString());
        }
    }

    /**
     * Create a trimmer that cut bases with a quality level above mean quality -
     * standard deviation, this class autoguess reads encoding quality
     *
     * @param inputPath the input file path
     * @throws IOException
     */
    public StreamingTrimmer(Path inputPath) throws IOException {
        this.inputPath = inputPath;
        BufferedReader reader = java.nio.file.Files.newBufferedReader(inputPath, CS);
        if (java.nio.file.Files.exists(this.inputPath)) {
            inputSupplier = Files.newReaderSupplier(this.inputPath.toFile(), CS);
        } else {
            throw new IOException("The input file does not exist: "
                    + inputPath.toString());
        }
        this.guessType();
    }

    /**
     * Analyze the input file and get statistics (needed to obtain QualMap
     * LenMap and stat)
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void analyze() throws Exception {
        fastqReader.stream(inputSupplier, new StreamListener() {

            @Override
            public void fastq(Fastq fastq) {
                int len = fastq.getSequence().length();
                if (lenMap.get(len) == null) {
                    Long a = new Long(1);
                    lenMap.put(len, a);
                } else {
                    Long freq = lenMap.get(len);
                    lenMap.put(len, freq + 1);
                }
                QualityFeature qualityFeature = FastqTools.createQualityScores(fastq);
                List<Integer> qList = qualityFeature.getQualities();
                for (Integer x : qList) {
                    if (qualMap.get(x) == null) {
                        Long a = new Long(1);
                        qualMap.put(x, a);
                    } else {
                        Long freq = qualMap.get(x);
                        qualMap.put(x, freq + 1);
                    }
                }
            }
        });
    }

    /**
     * Return a Map with each quality and the number of bases that have that
     * quality
     *
     * @return Map<quality, number of bases with that quality>
     */
    public Map<Integer, Long> getQualMap() {
        return qualMap;
    }

    /**
     * Return a Map with each sequence length and the number of sequences that
     * have that length
     *
     * @return Map<sequence length, number of sequences with that length>
     */
    public Map<Integer, Long> getLenMap() {
        return lenMap;
    }

    /**
     * Return a Map with this statistics: Length.mean = mean length of the
     * sequences in the input file Length.SD = standard deviation of the
     * sequences length Quality.mean = per base quality mean Quality.SD = per
     * base quality standard deviation
     */
    public Map<String, Double> getStat() throws Exception {

        double numBases = 0;
        double numSeq = 0;
        double lenSumOfSq = 0;
        for (Integer x : lenMap.keySet()) {
            numBases += x * lenMap.get(x);
            numSeq += lenMap.get(x);
            lenSumOfSq += (x * x) * lenMap.get(x);
        }
        double lenVariance = Descriptive.variance((int) numSeq, numBases, lenSumOfSq);
        double lenStDev = Descriptive.standardDeviation(lenVariance);
        double lenMean = numBases / numSeq;

        double totQual = 0;
        double qualSumOfSq = 0;
        for (Integer x : qualMap.keySet()) {
            totQual += x * qualMap.get(x);
            qualSumOfSq += (x * x) * qualMap.get(x);
        }
        double qualVariance = Descriptive.variance((int) numBases, totQual, qualSumOfSq);
        double qualStDev = Descriptive.standardDeviation(qualVariance);
        double qualMean = totQual / numBases;

        stat.put("Length.mean", lenMean);
        stat.put("Length.SD", lenStDev);
        stat.put("Quality.mean", qualMean);
        stat.put("Quality.SD", qualStDev);

        return stat;
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, Map<String, Double>> getDistribution() throws Exception {
        final Map<Integer, Map<String, Double>> distribution = new TreeMap<>();
        final Map<Integer, Map<Integer, Long>> bases = new TreeMap<>();
        final Map<Integer, Long> tabBase = new TreeMap<>();
        final Map<Integer, Long> freqTab = new TreeMap<>();
        if (lenMap == null) {
            throw new Exception("You have to use analyze() method first!");
        } else {
            DoubleArrayList lenList = new DoubleArrayList();
            for (Integer x : lenMap.keySet()) {
                lenList.add(x);
            }
            final int maxLen = (int) Descriptive.max(lenList);

            fastqReader.stream(inputSupplier, new StreamListener() {

                @Override
                public void fastq(Fastq fastq) {
                    QualityFeature qualityFeature = FastqTools.createQualityScores(fastq);
                    List<Integer> qList = qualityFeature.getQualities();
                    Map<Integer, Long> tabBase = null;
                    try {
                        for (int n = 0; n < maxLen; n++) {
                            int qual = qList.get(n);
                            int b = n + 1;
                            if (bases.get(b) == null) {
                                tabBase = new TreeMap<>();
                                bases.put(b, tabBase);
                            } else {
                                tabBase = bases.get(b);
                            }
                            if (tabBase.get(qual) == null) {
                                Long l = new Long(1);
                                tabBase.put(qual, l);
                            } else {
                                Long l = tabBase.get(qual);
                                tabBase.put(qual, l + 1);
                            }
                        }
//                        int qual = qList.get(pos);
//                        Long get = freqTab.get(qual);
//                        if (get == null) {
//                            Long l = new Long(1);
//                            freqTab.put(qual, l);
//                        } else {
//                            freqTab.put(qual, get + 1);
//                        }
                    } catch (IndexOutOfBoundsException iobE) {
                    }
                }
            });
            for (Integer x : bases.keySet()) {
                Long total = new Long(0);
                for (Long l : bases.get(x).values()) {
                    total += l;
                }
                DoubleArrayList contr = new DoubleArrayList();
                for (Integer y : bases.get(x).keySet()) {
                    Long l = bases.get(x).get(y);
                    double d = (double) l / total * 100;
                    int fr = (int) d;
                    for (int n = 1; n <= fr; n++) {
                        contr.add(y);
                    }
                }
                Map<String, Double> val = new TreeMap<>();
                double min = Descriptive.min(contr);
                val.put("min", min);
                double q1 = Descriptive.quantile(contr, 0.25);
                val.put("q1", q1);
                double mean = Descriptive.mean(contr);
                val.put("mean", mean);
                double median = Descriptive.median(contr);
                val.put("median", median);
                double q3 = Descriptive.quantile(contr, 0.75);
                val.put("q3", q3);
                double max = Descriptive.max(contr);
                val.put("max", max);
                distribution.put(x, val);
            }

            return distribution;
        }
    }

    /**
     * Trim input sequences and write them into a file in the specified
     * outputPath
     *
     * @param cutOff the quality cut off
     * @param offSet the number of bases that have to be trimmed at the
     * beginning of every sequence
     * @param minLength the minimum length that a sequence have to be
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void trim(int cutOff, int offSet, int minLength,Path outputPath) throws IOException {
        this.outputPath = outputPath;
        final BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                outputPath, CS);
        final int CUTOFF = cutOff;
        final int OFFSET = offSet;
        final int MINLENGTH = minLength;

        fastqReader.stream(inputSupplier, new StreamListener() {

            @Override
            public void fastq(final Fastq fastq) {
                int len = fastq.getSequence().length();
                String description = fastq.getDescription();
                String sequence = fastq.getSequence();
                String quality = fastq.getQuality();
                QualityFeature qualityFeature = FastqTools.createQualityScores(fastq);
                List<Integer> qList = qualityFeature.getQualities();
                int end = len;
                for (int n = (len - 1); n >= OFFSET; n--) {
                    List<Integer> subList = qList.subList(n, end);
                    int level = 0;
                    for (Integer x : subList) {
                        int val = x - CUTOFF;
                        level += val;
                    }
                    if (level < 0) {
                        end = n;
                    }
                }
                try {
                    quality = quality.substring(OFFSET, end);
                    sequence = sequence.substring(OFFSET, end);
                    if (quality.length() >= MINLENGTH) {
                        writer.write("@" + description + "\n" + sequence + "\n" + "+" + "\n" + quality + "\n");
                        writer.flush();
                    }
                } catch (IOException IOe) {
                    System.err.println("Cannot write the output file");
                }
            }
        });
        writer.close();
    }

    /**
     * Guess the encoding type of the fastq file
     */
    private void guessType() throws IOException {
        BufferedReader reader = java.nio.file.Files.newBufferedReader(this.inputPath, CS);
        Scanner scan = new Scanner(reader);
        DoubleArrayList dList = new DoubleArrayList();
        String read = null;
        int seqLine = 0;
        int line = 0;
        while (scan.hasNext()) {
            String id = scan.nextLine();
            line++;
            if (id.startsWith("@") == false) {
                throw new IOException("First string is not ad ID string: " + id + "\n at line: " + line);
            } else {
                read = scan.nextLine();
                line++;
                seqLine = 0;
                while (read.toLowerCase().startsWith("a") || read.toLowerCase().startsWith("c")
                        || read.toLowerCase().startsWith("t") || read.toLowerCase().startsWith("g")
                        || read.toLowerCase().startsWith("n")) {
                    seqLine++;
                    read = scan.nextLine();
                    line++;
                }
            }
            String sep = read;
            if (sep.startsWith("+") == false) {
                throw new IOException("The sequence/quality separator is not valid: " + sep + "\n at line: " + line);
            } else {
                String qualitySeg = scan.nextLine();
                line++;
                StringBuilder qualityBuild = new StringBuilder();
                qualityBuild.append(qualitySeg);
                while (seqLine > 1) {
                    qualityBuild.append(qualitySeg);
                    qualitySeg = scan.nextLine();
                    line++;
                    seqLine--;
                }
                String quality = qualityBuild.toString();
                for (int n = 0; n < quality.length(); n++) {
                    int x = (int) quality.charAt(n);
                    dList.add(x);
                }
                double offSet = Descriptive.min(dList);
                double upSet = Descriptive.max(dList);
                if (offSet < 33) {
                    throw new IOException("Cannot determine fastq type, offest < 33");
                } else if (offSet < 59) {
                    this.fastqReader = new SangerFastqReader();
                    this.fastqWriter = new SangerFastqWriter();
                    this.variant = FastqVariant.FASTQ_SANGER;
                    break;
                } else if (offSet < 64 && upSet > 73) {
                    this.fastqReader = new SolexaFastqReader();
                    this.fastqWriter = new SolexaFastqWriter();
                    this.variant = FastqVariant.FASTQ_SOLEXA;
                    break;
                } else if (line == 400 || scan.hasNext() == false) {
                    this.fastqReader = new IlluminaFastqReader();
                    this.fastqWriter = new IlluminaFastqWriter();
                    this.variant = FastqVariant.FASTQ_ILLUMINA;
                    break;
                } else {
                    continue;
                }
            }
        }
    }

    public void toFASTA(Path inputPath, Path outputPath) throws IOException {
        BufferedReader reader = java.nio.file.Files.newBufferedReader(inputPath, CS);
        BufferedWriter writer = java.nio.file.Files.newBufferedWriter(outputPath, CS);
        try {
            String line = null;
            while ((line = reader.readLine()) != null) {

                String id = null;
                String sequence = null;
                if (line.startsWith("@")) {
                    id = line.replace("@", ">");
                    writer.write(id + "\n");
                } else {
                    throw new IOException("File input format not correct");
                }
                sequence = reader.readLine().toLowerCase();
                int lineSeq = 0;
                while (sequence.startsWith("a") || sequence.startsWith("c")
                        || sequence.startsWith("g") || sequence.startsWith("t")
                        || sequence.startsWith("n")) {
                    writer.write(sequence.toUpperCase() + "\n");
                    writer.flush();
                    sequence = reader.readLine().toLowerCase();
                    lineSeq++;
                }
                line = sequence;
                if (line.startsWith("+")) {
                    while (lineSeq > 0) {
                        reader.readLine();
                        lineSeq--;
                    }
                    continue;
                } else {
                    throw new IOException("Sequence fastq file is not well formatted");
                }
            }
        } catch (IOException IOe) {
            System.err.println("An error occurred when converting to FASTA: " + IOe.getMessage());
        } finally {
            writer.close();
        }
    }
}
