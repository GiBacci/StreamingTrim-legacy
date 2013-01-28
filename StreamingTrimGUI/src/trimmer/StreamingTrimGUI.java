/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package trimmer;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
import com.google.common.io.InputSupplier;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.biojava3.core.sequence.features.QualityFeature;
import org.biojava3.sequencing.io.fastq.Fastq;
import org.biojava3.sequencing.io.fastq.FastqReader;
import org.biojava3.sequencing.io.fastq.FastqTools;
import org.biojava3.sequencing.io.fastq.StreamListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.*;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCalculator;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.*;
import org.jfree.ui.Layer;

/**
 *
 * @author giovanni
 */
public class StreamingTrimGUI extends javax.swing.JFrame {

    private Path input = null;
    private Path output = null;
    private Path fastaOutput = null;
    private String readsType = null;
    private StreamingTrimmer trimmer = null;
    private StreamingTrimmer outTrimmer = null;
    private int cutOff = 0;
    private int offSet = 0;
    private int minLength = 1;
    private Map<String, Double> stats = null;
    private Map<String, Double> outStats = null;
    private JPanel boxPanel = null;
    private JPanel devPanel = null;
    private JPanel lenPanel = null;
    private JPanel boxPanelTrimmed = null;
    private JPanel devPanelTrimmed = null;
    private JPanel lenPanelTrimmed = null;
    private boolean plotted = false;
    private boolean trimPlotted = false;
    private PropertyChangeListener listener = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            progBars.setMaximum(100);
            progBars.setMinimum(0);
            if ("progress" == pce.getPropertyName()) {
                int progress = (Integer) pce.getNewValue();
                progBars.setStringPainted(true);
                progBars.setValue(progress);
                progBars.setString(pce.getNewValue().toString() + "%");
            }
            if ("state" == pce.getPropertyName() && progBars.getValue() == 100) {
                progBars.setStringPainted(false);
                progBars.setValue(0);
            }
        }
    };
    private ActionListener plotListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent ae) {
            switch (ae.getActionCommand()) {
                case "box":
                    plotPanel1.removeAll();
                    plotPanelTrimmed1.removeAll();
                    plotPanel1.add(boxPanel);
                    if (boxPanelTrimmed != null) {
                        plotPanelTrimmed1.add(boxPanelTrimmed);
                    }
                    plotPanel1.getParent().validate();
                    plotPanelTrimmed1.getParent().validate();
                    plotPanel1.repaint();
                    plotPanelTrimmed1.repaint();
                    break;
                case "dev":
                    plotPanel1.removeAll();
                    plotPanelTrimmed1.removeAll();
                    plotPanel1.add(devPanel);
                    if (devPanelTrimmed != null) {
                        plotPanelTrimmed1.add(devPanelTrimmed);
                    }
                    plotPanel1.getParent().validate();
                    plotPanelTrimmed1.getParent().validate();
                    plotPanel1.repaint();
                    plotPanelTrimmed1.repaint();
                    break;
                case "inp":
                    setProperty(stats);
                    break;
                case "trim":
                    setProperty(outStats);
                    break;
                case "qual":
                    devRadio.setEnabled(true);
                    boxRadio.setEnabled(true);
                    plotPanel1.removeAll();
                    plotPanelTrimmed1.removeAll();
                    if (boxRadio.isSelected()) {
                        plotPanel1.add(boxPanel);
                        if (boxPanelTrimmed != null) {
                            plotPanelTrimmed1.add(boxPanelTrimmed);
                        }
                        plotPanel1.getParent().validate();
                        plotPanelTrimmed1.getParent().validate();
                        plotPanel1.repaint();
                        plotPanelTrimmed1.repaint();
                    } else if (devRadio.isSelected()) {
                        plotPanel1.removeAll();
                        plotPanelTrimmed1.removeAll();
                        plotPanel1.add(devPanel);
                        if (devPanelTrimmed != null) {
                            plotPanelTrimmed1.add(devPanelTrimmed);
                        }
                        plotPanel1.getParent().validate();
                        plotPanelTrimmed1.getParent().validate();
                        plotPanel1.repaint();
                        plotPanelTrimmed1.repaint();
                    }
                    break;
                case "len":
                    devRadio.setEnabled(false);
                    boxRadio.setEnabled(false);
                    plotPanel1.removeAll();
                    plotPanelTrimmed1.removeAll();
                    plotPanel1.add(lenPanel);
                    if (devPanelTrimmed != null) {
                        plotPanelTrimmed1.add(lenPanelTrimmed);
                    }
                    plotPanel1.getParent().validate();
                    plotPanelTrimmed1.getParent().validate();
                    plotPanel1.repaint();
                    plotPanelTrimmed1.repaint();
                    break;
            }
        }
    };

    /**
     * Creates new form StreamingTrimGUI
     */
    public StreamingTrimGUI() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        readsTyp = new javax.swing.ButtonGroup();
        advancedOption = new javax.swing.JFrame();
        cutPanel = new javax.swing.JPanel();
        checkCuToFF = new javax.swing.JCheckBox();
        insCutOff = new javax.swing.JTextField();
        checkOffSet = new javax.swing.JCheckBox();
        insOffSet = new javax.swing.JTextField();
        checkMinLen = new javax.swing.JCheckBox();
        insMinLeng = new javax.swing.JTextField();
        functionGroup = new javax.swing.ButtonGroup();
        fileAlredyExist = new javax.swing.JOptionPane();
        exception = new javax.swing.JOptionPane();
        infoFrame = new javax.swing.JFrame();
        infoScroll = new javax.swing.JScrollPane();
        textInfo = new javax.swing.JTextPane();
        plotFrame = new javax.swing.JFrame();
        plotPanel = new javax.swing.JPanel();
        plotPanel1 = new javax.swing.JPanel();
        plotPanelTrimmed = new javax.swing.JPanel();
        plotPanelTrimmed1 = new javax.swing.JPanel();
        plotOptionPanel = new javax.swing.JPanel();
        boxRadio = new javax.swing.JRadioButton();
        devRadio = new javax.swing.JRadioButton();
        qualDistr = new javax.swing.JRadioButton();
        lenDistr = new javax.swing.JRadioButton();
        plotTypeGroup = new javax.swing.ButtonGroup();
        propGroup = new javax.swing.ButtonGroup();
        typePlotGroup = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        qualLab = new javax.swing.JLabel();
        lengthLab = new javax.swing.JLabel();
        meanLen = new javax.swing.JLabel();
        stQual = new javax.swing.JLabel();
        meanQual = new javax.swing.JLabel();
        stLen = new javax.swing.JLabel();
        stDev = new javax.swing.JLabel();
        mean = new javax.swing.JLabel();
        propInput = new javax.swing.JRadioButton();
        propTrim = new javax.swing.JRadioButton();
        readsTypePanel = new javax.swing.JPanel();
        guessButton = new javax.swing.JRadioButton();
        solexaButton = new javax.swing.JRadioButton();
        sangerButton = new javax.swing.JRadioButton();
        illuminaButton = new javax.swing.JRadioButton();
        progBars = new javax.swing.JProgressBar();
        inputFilePanel = new javax.swing.JPanel();
        inputLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        trimToFasta = new javax.swing.JCheckBox();
        trimmButton = new javax.swing.JButton();
        plotButton = new javax.swing.JButton();
        analyseButton = new javax.swing.JButton();
        convert = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openItem = new javax.swing.JMenuItem();
        exitMenu = new javax.swing.JMenuItem();
        plotMenu = new javax.swing.JMenu();
        pltWin = new javax.swing.JMenuItem();
        advOption = new javax.swing.JMenuItem();
        infoMenu = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();

        fileChooser.setFont(new java.awt.Font("URW Gothic L", 0, 15)); // NOI18N

        advancedOption.setTitle("Advanced Option");
        advancedOption.setMinimumSize(new java.awt.Dimension(230, 153));
        advancedOption.setResizable(false);

        cutPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        cutPanel.setMaximumSize(new java.awt.Dimension(230, 153));
        cutPanel.setMinimumSize(new java.awt.Dimension(230, 153));

        checkCuToFF.setText("Cutoff");
        checkCuToFF.setToolTipText("<html>If deselected:<br>\nDefault =  mean quality - standard deviation");
        checkCuToFF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkCuToFFActionPerformed(evt);
            }
        });

        insCutOff.setEditable(false);
        insCutOff.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        insCutOff.setEnabled(false);

        checkOffSet.setText("Offset");
        checkOffSet.setToolTipText("<html>If deselected<br>\nDefault = 0");
        checkOffSet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkOffSetActionPerformed(evt);
            }
        });

        insOffSet.setEditable(false);
        insOffSet.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        insOffSet.setEnabled(false);

        checkMinLen.setText("Minimum Length");
        checkMinLen.setToolTipText("<html>If deselected:<br>\nDefault = 1");
        checkMinLen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkMinLenActionPerformed(evt);
            }
        });

        insMinLeng.setEditable(false);
        insMinLeng.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        insMinLeng.setEnabled(false);

        javax.swing.GroupLayout cutPanelLayout = new javax.swing.GroupLayout(cutPanel);
        cutPanel.setLayout(cutPanelLayout);
        cutPanelLayout.setHorizontalGroup(
            cutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cutPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(cutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkOffSet)
                    .addComponent(checkMinLen)
                    .addComponent(checkCuToFF))
                .addGap(27, 27, 27)
                .addGroup(cutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(insOffSet, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE)
                    .addComponent(insCutOff, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(insMinLeng))
                .addContainerGap(23, Short.MAX_VALUE))
        );
        cutPanelLayout.setVerticalGroup(
            cutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cutPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(cutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkCuToFF)
                    .addComponent(insCutOff, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addGroup(cutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkOffSet)
                    .addComponent(insOffSet, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(29, 29, 29)
                .addGroup(cutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkMinLen)
                    .addComponent(insMinLeng, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout advancedOptionLayout = new javax.swing.GroupLayout(advancedOption.getContentPane());
        advancedOption.getContentPane().setLayout(advancedOptionLayout);
        advancedOptionLayout.setHorizontalGroup(
            advancedOptionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(cutPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        advancedOptionLayout.setVerticalGroup(
            advancedOptionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(cutPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        fileAlredyExist.setFont(new java.awt.Font("URW Gothic L", 0, 15)); // NOI18N

        infoScroll.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        textInfo.setContentType("text/html"); // NOI18N
        textInfo.setEditable(false);
        textInfo.setFont(new java.awt.Font("URW Gothic L", 0, 15)); // NOI18N
        textInfo.setText("<html>\n<h1 style=\"text-align:center;\">Developed by:</h1>\n<p style=\"text-align:center;font-size:20\"><a  href=\"mailto:giovanni.bacci@unifi.it\"><b>Giovanni Bacci*</b></a></p>\n<p style=\"text-align:center;font-size:12;\"><a style=\"color:red\" href=\"http://www.unifi.it/dblage/mdswitch.html\">*University of Florence</a><br>\n<a style=\"color:red\" href=\"http://sito.entecra.it/portale/cra_dati_istituto.php?id=202\">*CRA-RPS</a><br>\n<a style=\"color:red\" href=\"http://www.unifi.it/dbefcb/mdswitch.html\">*Combo - <em> Florence computational biology group</a></em></p>\n\n");
        textInfo.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                textInfoHyperlinkUpdate(evt);
            }
        });
        infoScroll.setViewportView(textInfo);

        javax.swing.GroupLayout infoFrameLayout = new javax.swing.GroupLayout(infoFrame.getContentPane());
        infoFrame.getContentPane().setLayout(infoFrameLayout);
        infoFrameLayout.setHorizontalGroup(
            infoFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoFrameLayout.createSequentialGroup()
                .addComponent(infoScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
        infoFrameLayout.setVerticalGroup(
            infoFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(infoScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 217, Short.MAX_VALUE)
        );

        plotFrame.setTitle("Plot Window");
        plotFrame.setResizable(false);

        plotPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Raw data", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("URW Gothic L", 0, 14))); // NOI18N

        plotPanel1.setBorder(null);
        plotPanel1.setPreferredSize(new java.awt.Dimension(300, 200));

        javax.swing.GroupLayout plotPanel1Layout = new javax.swing.GroupLayout(plotPanel1);
        plotPanel1.setLayout(plotPanel1Layout);
        plotPanel1Layout.setHorizontalGroup(
            plotPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 313, Short.MAX_VALUE)
        );
        plotPanel1Layout.setVerticalGroup(
            plotPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 249, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout plotPanelLayout = new javax.swing.GroupLayout(plotPanel);
        plotPanel.setLayout(plotPanelLayout);
        plotPanelLayout.setHorizontalGroup(
            plotPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(plotPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 313, Short.MAX_VALUE)
        );
        plotPanelLayout.setVerticalGroup(
            plotPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(plotPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(plotPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE))
        );

        plotPanelTrimmed.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Trimmed data", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("URW Gothic L", 0, 14))); // NOI18N

        plotPanelTrimmed1.setBorder(null);
        plotPanelTrimmed1.setPreferredSize(new java.awt.Dimension(300, 200));

        javax.swing.GroupLayout plotPanelTrimmed1Layout = new javax.swing.GroupLayout(plotPanelTrimmed1);
        plotPanelTrimmed1.setLayout(plotPanelTrimmed1Layout);
        plotPanelTrimmed1Layout.setHorizontalGroup(
            plotPanelTrimmed1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 313, Short.MAX_VALUE)
        );
        plotPanelTrimmed1Layout.setVerticalGroup(
            plotPanelTrimmed1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout plotPanelTrimmedLayout = new javax.swing.GroupLayout(plotPanelTrimmed);
        plotPanelTrimmed.setLayout(plotPanelTrimmedLayout);
        plotPanelTrimmedLayout.setHorizontalGroup(
            plotPanelTrimmedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(plotPanelTrimmed1, javax.swing.GroupLayout.DEFAULT_SIZE, 313, Short.MAX_VALUE)
        );
        plotPanelTrimmedLayout.setVerticalGroup(
            plotPanelTrimmedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(plotPanelTrimmedLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(plotPanelTrimmed1, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE))
        );

        plotOptionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Plot Options", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("URW Gothic L", 0, 14))); // NOI18N

        plotTypeGroup.add(boxRadio);
        boxRadio.setText("Box Plot");
        boxRadio.setEnabled(false);

        plotTypeGroup.add(devRadio);
        devRadio.setSelected(true);
        devRadio.setText("Deviation Plot");
        devRadio.setEnabled(false);

        typePlotGroup.add(qualDistr);
        qualDistr.setSelected(true);
        qualDistr.setText("Quality Distribution");
        qualDistr.setEnabled(false);

        typePlotGroup.add(lenDistr);
        lenDistr.setText("Length Distribution");
        lenDistr.setEnabled(false);

        javax.swing.GroupLayout plotOptionPanelLayout = new javax.swing.GroupLayout(plotOptionPanel);
        plotOptionPanel.setLayout(plotOptionPanelLayout);
        plotOptionPanelLayout.setHorizontalGroup(
            plotOptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, plotOptionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(plotOptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(qualDistr)
                    .addComponent(lenDistr))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 47, Short.MAX_VALUE)
                .addGroup(plotOptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(devRadio)
                    .addComponent(boxRadio))
                .addGap(23, 23, 23))
        );
        plotOptionPanelLayout.setVerticalGroup(
            plotOptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(plotOptionPanelLayout.createSequentialGroup()
                .addGroup(plotOptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(devRadio)
                    .addComponent(qualDistr))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(plotOptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(boxRadio)
                    .addComponent(lenDistr))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout plotFrameLayout = new javax.swing.GroupLayout(plotFrame.getContentPane());
        plotFrame.getContentPane().setLayout(plotFrameLayout);
        plotFrameLayout.setHorizontalGroup(
            plotFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, plotFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(plotFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(plotFrameLayout.createSequentialGroup()
                        .addComponent(plotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(plotPanelTrimmed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(plotFrameLayout.createSequentialGroup()
                        .addComponent(plotOptionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        plotFrameLayout.setVerticalGroup(
            plotFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(plotFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(plotOptionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(plotFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(plotPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(plotPanelTrimmed, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("StreamingTrim");
        setResizable(false);

        jPanel1.setBackground(javax.swing.UIManager.getDefaults().getColor("CheckBoxMenuItem.selectionBackground"));
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Reads Properties", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("URW Gothic L", 1, 14))); // NOI18N

        qualLab.setFont(new java.awt.Font("URW Gothic L", 1, 14)); // NOI18N
        qualLab.setText("Quality");
        qualLab.setBorder(null);

        lengthLab.setFont(new java.awt.Font("URW Gothic L", 1, 14)); // NOI18N
        lengthLab.setText("Length");
        lengthLab.setBorder(null);

        meanLen.setFont(new java.awt.Font("URW Gothic L", 1, 14)); // NOI18N
        meanLen.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        meanLen.setText("<NA>");
        meanLen.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        meanLen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        stQual.setFont(new java.awt.Font("URW Gothic L", 1, 14)); // NOI18N
        stQual.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        stQual.setText("<NA>");
        stQual.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        stQual.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        meanQual.setFont(new java.awt.Font("URW Gothic L", 1, 14)); // NOI18N
        meanQual.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        meanQual.setText("<NA>");
        meanQual.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        meanQual.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        stLen.setFont(new java.awt.Font("URW Gothic L", 1, 14)); // NOI18N
        stLen.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        stLen.setText("<NA>");
        stLen.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        stLen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        stDev.setBackground(new java.awt.Color(254, 254, 254));
        stDev.setFont(new java.awt.Font("URW Gothic L", 0, 14)); // NOI18N
        stDev.setForeground(new java.awt.Color(1, 1, 1));
        stDev.setText("St. Dev.");
        stDev.setBorder(null);

        mean.setBackground(new java.awt.Color(254, 254, 254));
        mean.setFont(new java.awt.Font("URW Gothic L", 0, 14)); // NOI18N
        mean.setForeground(new java.awt.Color(1, 1, 1));
        mean.setText("Mean");
        mean.setBorder(null);

        propGroup.add(propInput);
        propInput.setFont(new java.awt.Font("URW Gothic L", 0, 12)); // NOI18N
        propInput.setSelected(true);
        propInput.setText("Input File");
        propInput.setEnabled(false);

        propGroup.add(propTrim);
        propTrim.setFont(new java.awt.Font("URW Gothic L", 0, 12)); // NOI18N
        propTrim.setText("Trimmed File");
        propTrim.setEnabled(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mean, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stDev))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lengthLab)
                    .addComponent(meanLen, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(stLen, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(meanQual)
                    .addComponent(qualLab)
                    .addComponent(stQual, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(propTrim)
                    .addComponent(propInput))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {lengthLab, meanLen, meanQual, qualLab, stLen, stQual});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lengthLab)
                    .addComponent(qualLab))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(propInput)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(propTrim))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(mean)
                            .addComponent(meanLen)
                            .addComponent(meanQual))
                        .addGap(6, 6, 6)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(stDev)
                            .addComponent(stLen)
                            .addComponent(stQual))))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {mean, meanLen, meanQual, stDev, stLen, stQual});

        readsTypePanel.setBackground(javax.swing.UIManager.getDefaults().getColor("CheckBoxMenuItem.selectionBackground"));
        readsTypePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Reads Type", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("URW Gothic L", 1, 14))); // NOI18N

        readsTyp.add(guessButton);
        guessButton.setFont(new java.awt.Font("Ubuntu", 0, 14)); // NOI18N
        guessButton.setSelected(true);
        guessButton.setText("<guess>");

        readsTyp.add(solexaButton);
        solexaButton.setFont(new java.awt.Font("Ubuntu", 0, 14)); // NOI18N
        solexaButton.setText("Solexa");

        readsTyp.add(sangerButton);
        sangerButton.setFont(new java.awt.Font("Ubuntu", 0, 14)); // NOI18N
        sangerButton.setText("Sanger");

        readsTyp.add(illuminaButton);
        illuminaButton.setFont(new java.awt.Font("Ubuntu", 0, 14)); // NOI18N
        illuminaButton.setText("Illumina");

        javax.swing.GroupLayout readsTypePanelLayout = new javax.swing.GroupLayout(readsTypePanel);
        readsTypePanel.setLayout(readsTypePanelLayout);
        readsTypePanelLayout.setHorizontalGroup(
            readsTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(readsTypePanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(readsTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(readsTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(solexaButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(illuminaButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 76, Short.MAX_VALUE)
                        .addComponent(sangerButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(guessButton))
                .addGap(45, 45, 45))
        );

        readsTypePanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {guessButton, illuminaButton, sangerButton, solexaButton});

        readsTypePanelLayout.setVerticalGroup(
            readsTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(readsTypePanelLayout.createSequentialGroup()
                .addComponent(sangerButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(illuminaButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(solexaButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(guessButton)
                .addGap(0, 7, Short.MAX_VALUE))
        );

        progBars.setBackground(new java.awt.Color(169, 205, 218));
        progBars.setFont(new java.awt.Font("URW Gothic L", 1, 12)); // NOI18N
        progBars.setForeground(new java.awt.Color(1, 1, 1));
        progBars.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        inputFilePanel.setBackground(javax.swing.UIManager.getDefaults().getColor("CheckBoxMenuItem.selectionBackground"));
        inputFilePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Input File", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("URW Gothic L", 1, 14))); // NOI18N
        inputFilePanel.setMaximumSize(new java.awt.Dimension(10, 50));

        javax.swing.GroupLayout inputFilePanelLayout = new javax.swing.GroupLayout(inputFilePanel);
        inputFilePanel.setLayout(inputFilePanelLayout);
        inputFilePanelLayout.setHorizontalGroup(
            inputFilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(inputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        inputFilePanelLayout.setVerticalGroup(
            inputFilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(inputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE)
        );

        jPanel2.setBackground(javax.swing.UIManager.getDefaults().getColor("CheckBoxMenuItem.selectionBackground"));
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Controls", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("URW Gothic L", 1, 14))); // NOI18N

        trimToFasta.setFont(new java.awt.Font("URW Gothic L", 0, 12)); // NOI18N
        trimToFasta.setText("Trim to FASTA");
        trimToFasta.setEnabled(false);
        trimToFasta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trimToFastaActionPerformed(evt);
            }
        });

        trimmButton.setText("Trim");
        trimmButton.setEnabled(false);
        trimmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trimmButtonActionPerformed(evt);
            }
        });

        plotButton.setText("Plot");
        plotButton.setEnabled(false);
        plotButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                plotButtonActionPerformed(evt);
            }
        });

        analyseButton.setText("Analyze");
        analyseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                analyseButtonActionPerformed(evt);
            }
        });

        convert.setText("FASTA");
        convert.setEnabled(false);
        convert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                convertActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(convert)
                        .addGap(18, 18, 18)
                        .addComponent(plotButton))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(analyseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(trimmButton)
                        .addGap(34, 34, 34)
                        .addComponent(trimToFasta)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {analyseButton, convert, plotButton, trimmButton});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(analyseButton)
                        .addComponent(trimmButton))
                    .addComponent(trimToFasta))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(convert)
                    .addComponent(plotButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {analyseButton, convert, plotButton, trimmButton});

        fileMenu.setText("File");

        openItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openItem.setText("Open file");
        openItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openItemActionPerformed(evt);
            }
        });
        fileMenu.add(openItem);

        exitMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        exitMenu.setText("Exit");
        exitMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenu);

        jMenuBar1.add(fileMenu);

        plotMenu.setText("Window");

        pltWin.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        pltWin.setText("Show plot window");
        pltWin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pltWinActionPerformed(evt);
            }
        });
        plotMenu.add(pltWin);

        advOption.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        advOption.setText("Show advanced options");
        advOption.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                advOptionActionPerformed(evt);
            }
        });
        plotMenu.add(advOption);

        jMenuBar1.add(plotMenu);

        infoMenu.setText("?");

        jMenuItem1.setText("info");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        infoMenu.add(jMenuItem1);

        jMenuBar1.add(infoMenu);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(progBars, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(readsTypePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(inputFilePanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(inputFilePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(readsTypePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(progBars, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private class AnalyzeWorker extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() {
            try {
                enableButtons(false);
                progBars.setString("Analyzing...");
                progBars.setIndeterminate(true);
                progBars.setStringPainted(true);
                trimmer.analyze();
                stats = trimmer.getStat();
                setProperty(stats);
                progBars.setIndeterminate(false);
                progBars.setStringPainted(false);
                enableButtons(true);
            } catch (Exception exc) {
                JOptionPane.showMessageDialog(exception, "Cannot read the specified input file.");
                progBars.setIndeterminate(false);
                progBars.setStringPainted(false);
                analyseButton.setEnabled(true);
            }
            return null;
        }
    }

    private class AnalyzeWorkerTrimmed extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() {
            try {
                enableButtons(false);
                progBars.setString("Analyzing trimmed file...");
                progBars.setIndeterminate(true);
                progBars.setStringPainted(true);
                outTrimmer.analyze();
                outStats = outTrimmer.getStat();
                setProperty(outStats);
                progBars.setIndeterminate(false);
                progBars.setStringPainted(false);
                enableButtons(true);
            } catch (Exception exc) {
                JOptionPane.showMessageDialog(exception, "Cannot read trimmed file.");
                progBars.setIndeterminate(false);
                progBars.setStringPainted(false);
                analyseButton.setEnabled(true);
            }
            return null;
        }
    }

    private void setProperty(Map<String, Double> stats) {
        for (String a : stats.keySet()) {
            switch (a) {
                case "Length.mean":
                    Double d = stats.get(a);
                    BigDecimal bd = new BigDecimal(d);
                    bd = bd.setScale(1, BigDecimal.ROUND_UP);
                    d = bd.doubleValue();
                    meanLen.setText(d.toString());
                    break;
                case "Length.SD":
                    Double dd = stats.get(a);
                    BigDecimal bdd = new BigDecimal(dd);
                    bdd = bdd.setScale(1, BigDecimal.ROUND_UP);
                    dd = bdd.doubleValue();
                    stLen.setText(dd.toString());
                    break;
                case "Quality.mean":
                    Double q = stats.get(a);
                    BigDecimal bdq = new BigDecimal(q);
                    bdq = bdq.setScale(2, BigDecimal.ROUND_UP);
                    q = bdq.doubleValue();
                    meanQual.setText(q.toString());
                    break;
                case "Quality.SD":
                    Double qq = stats.get(a);
                    BigDecimal bdqq = new BigDecimal(qq);
                    bdqq = bdqq.setScale(2, BigDecimal.ROUND_UP);
                    qq = bdqq.doubleValue();
                    stQual.setText(qq.toString());
                    break;
            }
        }
    }

    private void enableButtons(boolean enabled) {
        analyseButton.setEnabled(enabled);
        trimmButton.setEnabled(enabled);
        convert.setEnabled(enabled);
        trimToFasta.setEnabled(enabled);
        plotButton.setEnabled(enabled);
    }

    private void openTrimmer(Path in, String readsType) {
        if (guessButton.isSelected()) {
            this.readsType = null;
        } else if (illuminaButton.isSelected()) {
            this.readsType = "illumina";
        } else if (sangerButton.isSelected()) {
            this.readsType = "sanger";
        } else if (solexaButton.isSelected()) {
            this.readsType = "solexa";
        }
        try {
            if (readsType == null) {
                this.trimmer = new StreamingTrimmer(in);
            } else {
                this.trimmer = new StreamingTrimmer(in, readsType);
            }
        } catch (IOException IOe) {
            JOptionPane.showMessageDialog(exception, "Problems reading file.");
        }
    }

    private void openOutTrimmer(Path in, String readsType) {
        if (guessButton.isSelected()) {
            this.readsType = null;
        } else if (illuminaButton.isSelected()) {
            this.readsType = "illumina";
        } else if (sangerButton.isSelected()) {
            this.readsType = "sanger";
        } else if (solexaButton.isSelected()) {
            this.readsType = "solexa";
        }
        try {
            if (readsType == null) {
                this.outTrimmer = new StreamingTrimmer(in);
            } else {
                this.outTrimmer = new StreamingTrimmer(in, readsType);
            }
        } catch (IOException IOe) {
            JOptionPane.showMessageDialog(exception, "Problems reading trimmed file.");
        }
    }

    private void resetGUI() {
        //Reset della parte non grafica
        this.trimmer = null;
        this.outTrimmer = null;
        this.input = null;
        this.output = null;
        this.stats = null;
        this.outStats = null;
        this.plotted = false;
        this.trimPlotted = false;

        //Reset della parte grafica
        this.boxPanel = null;
        this.boxPanelTrimmed = null;
        this.devPanel = null;
        this.devPanelTrimmed = null;
        this.lenPanel = null;
        this.lenPanelTrimmed = null;
        this.plotPanel1.removeAll();
        this.plotPanel1.repaint();
        this.plotPanelTrimmed1.removeAll();
        this.plotPanelTrimmed1.repaint();

        //Reset dello status iniziale dei bottoni e delle finestre
        this.propInput.setEnabled(false);
        this.propTrim.setEnabled(false);
        this.propInput.setSelected(true);
        this.boxRadio.setEnabled(false);
        this.devRadio.setEnabled(false);
        this.qualDistr.setEnabled(false);
        this.qualDistr.setSelected(true);
        this.lenDistr.setEnabled(false);
        this.inputLabel.setText("");
        this.meanLen.setText("<NA>");
        this.meanQual.setText("<NA>");
        this.stLen.setText("<NA>");
        this.stQual.setText("<NA>");
    }
    private void openItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openItemActionPerformed
        FileFilter filter = new FileNameExtensionFilter("FastQ files", "fq", "fastq", "txt");
        fileChooser.setFileFilter(filter);
        int relVal = fileChooser.showDialog(this,"Open File");
        if (relVal == JFileChooser.APPROVE_OPTION) {
            resetGUI();
            File file = fileChooser.getSelectedFile();
            this.input = file.toPath();
            if (Files.exists(this.input)) {
                enableButtons(true);
                openTrimmer(this.input, this.readsType);

                if (this.input.getNameCount() > 3) {
                    String dirSep = System.getProperty("file.separator");
                    String inPath = this.input.subpath(0, 1).toString()
                            + dirSep + "..." + dirSep
                            + this.input.subpath((input.getNameCount() - 2), input.getNameCount()).toString();
                    inputLabel.setText(inPath);
                } else {
                    inputLabel.setText(this.input.toString());
                }
            } else {
                this.input = null;
                JOptionPane.showMessageDialog(exception, "Specified file does not exist.");
            }
        }
        fileChooser.resetChoosableFileFilters();
    }//GEN-LAST:event_openItemActionPerformed

    private void advOptionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_advOptionActionPerformed
        advancedOption.pack();
        advancedOption.setVisible(true);
    }//GEN-LAST:event_advOptionActionPerformed

    private void analyseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_analyseButtonActionPerformed
        if (this.input == null) {
            FileFilter filter = new FileNameExtensionFilter("FastQ files", "fq", "fastq", "txt");
            fileChooser.setFileFilter(filter);
            int relVal = fileChooser.showDialog(this, "Open File");
            if (relVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                this.input = file.toPath();
                openTrimmer(this.input, this.readsType);
                if (!Files.exists(this.input)) {
                    this.input = null;
                    JOptionPane.showMessageDialog(exception, "Specified file does not exist.");
                }
            }
        }
        fileChooser.resetChoosableFileFilters();

        if (this.input != null && Files.exists(this.input)) {
            AnalyzeWorker anWork = new AnalyzeWorker();
            anWork.execute();
            if (this.input.getNameCount() > 3) {
                String dirSep = System.getProperty("file.separator");
                String inPath = this.input.subpath(0, 1).toString()
                        + dirSep + "..." + dirSep
                        + this.input.subpath((input.getNameCount() - 2), input.getNameCount()).toString();
                inputLabel.setText(inPath);
            } else {
                inputLabel.setText(this.input.toString());
            }
        } else {
            this.input = null;
        }
    }//GEN-LAST:event_analyseButtonActionPerformed

    private void trimmButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trimmButtonActionPerformed

        FileFilter fqFilter = new FileNameExtensionFilter("FastQ files .fq", "fq");
        FileFilter fastqFilter = new FileNameExtensionFilter("Generic FastQ files .fastq", "fastq");
        String[] extensions = {".fq", ".fastq"};
        fileChooser.setFileFilter(fqFilter);
        fileChooser.setFileFilter(fastqFilter);
        int val = fileChooser.showDialog(this, "Save File");
        if (val == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            FileFilter selected = fileChooser.getFileFilter();
            Path p = file.toPath();
            for (String x : extensions) {
                if (p.toString().endsWith(x)) {
                    this.output = p;
                    break;
                } else if (selected.getDescription().contains(x)) {
                    String name = p.getFileName().toString() + x;
                    this.output = p.getParent().resolve(name);
                    continue;
                }
            }

            if (Files.exists(this.output)) {
                String[] options = {"Yes", "No"};
                int n = JOptionPane.showOptionDialog(fileAlredyExist, "File alredy exist!\n"
                        + "Do you want to override it?", "File alredy exist warning",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                if (n == JOptionPane.YES_OPTION) {
                } else {
                    this.output = null;
                }
            }

            if (this.output != null) {

                Runnable runner = new Runnable() {

                    @Override
                    public void run() {
                        enableButtons(false);
                        progBars.setIndeterminate(true);
                        progBars.setStringPainted(true);
                        try {
                            if (checkCuToFF.isSelected() == false) {
                                if (stats == null) {
                                    progBars.setString("Analyzing...");
                                    trimmer.analyze();
                                    stats = trimmer.getStat();
                                    setProperty(stats);
                                } else {
                                    progBars.setString("File already analyzed...");
                                    Thread.sleep(1000);
                                    progBars.setString("Skipping analysis...");
                                    Thread.sleep(1000);
                                }
                                double mean = stats.get("Quality.mean");
                                double sd = stats.get("Quality.SD");
                                BigDecimal bd = new BigDecimal(sd);
                                bd = bd.setScale(2, BigDecimal.ROUND_UP);
                                sd = bd.doubleValue();
                                cutOff = (int) (mean - sd);
                            } else {
                                cutOff = Integer.parseInt(insCutOff.getText());
                            }
                            if (checkMinLen.isSelected()) {
                                minLength = Integer.parseInt(insMinLeng.getText());
                            }
                            if (checkOffSet.isSelected()) {
                                offSet = Integer.parseInt(insOffSet.getText());
                            }
                            progBars.setString("Trimming...");
                            trimmer.trim(cutOff, offSet, minLength, output);
                            if (trimToFasta.isSelected()) {
                                progBars.setString("Converting...");
                                trimmer.toFASTA(output, fastaOutput);
                            }
                            progBars.setString("Analyzing trimmed file...");
                            openOutTrimmer(output, readsType);
                            outTrimmer.analyze();
                            outStats = outTrimmer.getStat();
                            setProperty(outStats);
                            propTrim.setSelected(true);
                            progBars.setIndeterminate(false);
                            progBars.setStringPainted(false);
                            enableButtons(true);
                            propInput.setActionCommand("inp");
                            propTrim.setActionCommand("trim");
                            propInput.addActionListener(plotListener);
                            propTrim.addActionListener(plotListener);
                            propInput.setEnabled(true);
                            propTrim.setEnabled(true);
                            trimPlotted = false;
                            plotPanelTrimmed1.removeAll();
                            plotPanelTrimmed1.repaint();

                        } catch (Exception exc) {
                            JOptionPane.showMessageDialog(exception, "Cannot trimm the specified input file.");
                            progBars.setIndeterminate(false);
                            progBars.setStringPainted(false);
                            analyseButton.setEnabled(true);
                        }
                    }
                };
                (new Thread(runner)).start();
            }
        }
        fileChooser.resetChoosableFileFilters();
    }//GEN-LAST:event_trimmButtonActionPerformed

    private void checkCuToFFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkCuToFFActionPerformed
        insCutOff.setEditable(checkCuToFF.isSelected());
        insCutOff.setEnabled(checkCuToFF.isSelected());
    }//GEN-LAST:event_checkCuToFFActionPerformed

    private void checkOffSetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkOffSetActionPerformed
        insOffSet.setEditable(checkOffSet.isSelected());
        insOffSet.setEnabled(checkOffSet.isSelected());
    }//GEN-LAST:event_checkOffSetActionPerformed

    private void checkMinLenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkMinLenActionPerformed
        insMinLeng.setEditable(checkMinLen.isSelected());
        insMinLeng.setEnabled(checkMinLen.isSelected());
    }//GEN-LAST:event_checkMinLenActionPerformed

    private void trimToFastaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trimToFastaActionPerformed
        if (trimToFasta.isSelected()) {
            FileFilter fastaFilter = new FileNameExtensionFilter("FATSA file .fasta", "fasta");
            FileFilter fnaFilter = new FileNameExtensionFilter("FASTA nucleotide file .fna", "fna");
            FileFilter fsaFilter = new FileNameExtensionFilter("Generic FASTA file .fsa", "fsa");
            String[] extensions = {".fasta", ".fna", ".fsa"};
            fileChooser.setFileFilter(fastaFilter);
            fileChooser.setFileFilter(fnaFilter);
            fileChooser.setFileFilter(fsaFilter);
            int relVal = fileChooser.showDialog(this, "Save FASTA file");
            if (relVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                FileFilter selected = fileChooser.getFileFilter();
                Path p = file.toPath();
                for (String x : extensions) {
                    if (p.toString().endsWith(x)) {
                        this.fastaOutput = p;
                        break;
                    } else if (selected.getDescription().contains(x)) {
                        String name = p.getFileName().toString() + x;
                        this.fastaOutput = p.getParent().resolve(name);
                        continue;
                    }
                }
                if (Files.exists(this.fastaOutput)) {
                    String[] options = {"Yes", "No"};
                    int n = JOptionPane.showOptionDialog(fileAlredyExist, "File alredy exist!\n"
                            + "Do you want to override it?", "File alredy exist warning",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                    if (n == JOptionPane.YES_OPTION) {
                    } else {
                        this.fastaOutput = null;
                        trimToFasta.setSelected(false);
                    }
                }
            } else {
                trimToFasta.setSelected(false);
            }
        }
        fileChooser.resetChoosableFileFilters();
    }//GEN-LAST:event_trimToFastaActionPerformed

    private void convertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_convertActionPerformed

        FileFilter fastaFilter = new FileNameExtensionFilter("FATSA file .fasta", "fasta");
        FileFilter fnaFilter = new FileNameExtensionFilter("FASTA nucleotide file .fna", "fna");
        FileFilter fsaFilter = new FileNameExtensionFilter("Generic FASTA file .fsa", "fsa");
        String[] extensions = {".fasta", ".fna", ".fsa"};
        fileChooser.setFileFilter(fastaFilter);
        fileChooser.setFileFilter(fnaFilter);
        fileChooser.setFileFilter(fsaFilter);

        int relVal = fileChooser.showDialog(this, "Save FASTA file");
        if (relVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            FileFilter selected = fileChooser.getFileFilter();
            Path p = file.toPath();
            for (String x : extensions) {
                if (p.toString().endsWith(x)) {
                    this.fastaOutput = p;
                    break;
                } else if (selected.getDescription().contains(x)) {
                    String name = p.getFileName().toString() + x;
                    this.fastaOutput = p.getParent().resolve(name);
                    continue;
                }
            }
        } else {
            this.fastaOutput = null;
        }

        fileChooser.resetChoosableFileFilters();
        if (this.fastaOutput != null
                && Files.exists(this.fastaOutput)) {
            String[] options = {"Yes", "No"};
            int n = JOptionPane.showOptionDialog(fileAlredyExist, "File alredy exist!\n"
                    + "Do you want to override it?", "File alredy exist warning",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (n == JOptionPane.YES_OPTION) {
            } else {
                this.fastaOutput = null;
            }
        }
        if (this.fastaOutput != null) {
            Runnable runner = new Runnable() {

                @Override
                public void run() {
                    try {
                        enableButtons(false);
                        progBars.setStringPainted(true);
                        progBars.setIndeterminate(true);
                        if (stats == null) {
                            progBars.setString("Checking file...");
                            trimmer.analyze();
                        } else {
                            progBars.setString("File already analyzed...");
                            Thread.sleep(1000);
                            progBars.setString("Skipping check...");
                            Thread.sleep(1000);
                        }
                        progBars.setString("Converting to FASTA...");
                        trimmer.toFASTA(input, fastaOutput);
                        progBars.setStringPainted(false);
                        progBars.setIndeterminate(false);
                        enableButtons(true);
                    } catch (Exception exc) {
                        JOptionPane.showMessageDialog(exception, "Cannot convert the specified input file.");
                        progBars.setIndeterminate(false);
                        progBars.setStringPainted(false);
                        analyseButton.setEnabled(true);
                    }
                }
            };
            Thread threadConvert = new Thread(runner);
            threadConvert.start();
        }
    }//GEN-LAST:event_convertActionPerformed
    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        infoFrame.pack();
        infoFrame.setVisible(true);
        textInfo.setOpaque(false);
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void textInfoHyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_textInfoHyperlinkUpdate
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            Desktop desktop = Desktop.getDesktop();
            try {
                if (evt.getURL().toString().contains("@")) {
                    desktop.mail(evt.getURL().toURI());
                } else {
                    desktop.browse(evt.getURL().toURI());
                }
            } catch (URISyntaxException USE) {
            } catch (IOException IOe) {
            }
        }
    }//GEN-LAST:event_textInfoHyperlinkUpdate

    private void exitMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuActionPerformed
        this.dispose();
    }//GEN-LAST:event_exitMenuActionPerformed

    private void pltWinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pltWinActionPerformed
        Point p = this.getLocation();
        int x = p.x + 420;
        int y = p.y - 30;
        plotFrame.setLocation(x, y);
        plotFrame.pack();
        plotFrame.setVisible(true);
    }//GEN-LAST:event_pltWinActionPerformed

    private void plotButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_plotButtonActionPerformed

        if (!plotFrame.isVisible()) {
            Point p = this.getLocation();
            int x = p.x + 420;
            int y = p.y - 30;
            plotFrame.setLocation(x, y);
        }
        plotFrame.pack();
        plotFrame.setVisible(true);

        devRadio.setActionCommand("dev");
        boxRadio.setActionCommand("box");
        qualDistr.setActionCommand("qual");
        lenDistr.setActionCommand("len");
        devRadio.addActionListener(plotListener);
        boxRadio.addActionListener(plotListener);
        qualDistr.addActionListener(plotListener);
        lenDistr.addActionListener(plotListener);

        SwingWorker<Void, Void> plotting = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                enableButtons(false);
                if (plotted == false) {
                    DeviationRenderer devRender = new DeviationRenderer(true, false);
                    devRender.setSeriesStroke(0, new BasicStroke(2F, 1, 1));
                    devRender.setSeriesFillPaint(0, new Color(58, 95, 205));
                    devRender.setSeriesPaint(0, new Color(77, 77, 255));

                    BoxAndWhiskerRenderer render = new BoxAndWhiskerRenderer();
                    render.setMeanVisible(false);
                    render.setFillBox(true);
                    render.setSeriesPaint(0, Color.BLUE);
                    render.setUseOutlinePaintForWhiskers(false);
                    render.setUseOutlinePaintForWhiskers(true);
                    render.setBaseOutlinePaint(Color.BLACK);

                    XYBarRenderer barRender = new XYBarRenderer(0.0);
                    barRender.setShadowVisible(false);
                    barRender.setSeriesPaint(0, Color.BLUE);

                    CategoryAxis axisX = new CategoryAxis();
                    axisX.setTickLabelsVisible(false);
                    NumberAxis axisY = new NumberAxis();
                    NumberAxis numAxisX = new NumberAxis();
                    NumberAxis lenAxisX = new NumberAxis();
                    NumberAxis lenAxisY = new NumberAxis();

                    IntervalMarker markerBAD = new IntervalMarker(0, 18, Color.getHSBColor(1.0f, 0.5f, 1.0f));
                    IntervalMarker markerNEUTRAL = new IntervalMarker(18, 30, Color.getHSBColor(0.1f, 0.5f, 1.0f));
                    IntervalMarker markerGOOD = new IntervalMarker(30, 70, Color.getHSBColor(0.3f, 0.5f, 1.0f));

                    CategoryPlot cPlot = new CategoryPlot();
                    cPlot.setRangeAxis(axisY);
                    cPlot.setDomainAxis(axisX);
                    cPlot.setRenderer(render);
                    cPlot.addRangeMarker(markerBAD, Layer.BACKGROUND);
                    cPlot.addRangeMarker(markerNEUTRAL, Layer.BACKGROUND);
                    cPlot.addRangeMarker(markerGOOD, Layer.BACKGROUND);

                    XYPlot xyPlot = new XYPlot();
                    xyPlot.setRangeAxis(axisY);
                    xyPlot.setDomainAxis(numAxisX);
                    xyPlot.setRenderer(devRender);
                    xyPlot.addRangeMarker(markerBAD, Layer.BACKGROUND);
                    xyPlot.addRangeMarker(markerNEUTRAL, Layer.BACKGROUND);
                    xyPlot.addRangeMarker(markerGOOD, Layer.BACKGROUND);

                    XYPlot xyBarPlot = new XYPlot();
                    xyBarPlot.setDomainAxis(lenAxisX);
                    xyBarPlot.setRangeAxis(lenAxisY);
                    xyBarPlot.setRenderer(barRender);
                    xyBarPlot.setRangeGridlinePaint(Color.DARK_GRAY);

                    progBars.setIndeterminate(true);
                    progBars.setStringPainted(true);
                    if (stats == null) {
                        progBars.setString("Analyzing...");
                        trimmer.analyze();
                        stats = trimmer.getStat();
                        setProperty(stats);
                    }
                    DoubleArrayList lenList = new DoubleArrayList();
                    for (Integer x : trimmer.getLenMap().keySet()) {
                        lenList.add(x);
                    }
                    progBars.setString("Begin deep analysis for plotting...");
                    Map<Integer, Map<String, Double>> dist = trimmer.getDistribution();
                    progBars.setIndeterminate(false);
                    progBars.setStringPainted(false);
                    setProgress(0);
                    final int maxLen = (int) Descriptive.max(lenList);
                    DefaultBoxAndWhiskerCategoryDataset dataset =
                            new DefaultBoxAndWhiskerCategoryDataset();
                    YIntervalSeriesCollection collection = new YIntervalSeriesCollection();
                    XYSeriesCollection xyCollection = new XYSeriesCollection();
                    XYSeries xySeries = new XYSeries("Length");
                    BoxAndWhiskerItem item = null;
                    YIntervalSeries series = new YIntervalSeries("Bases");
                    long total = 0;
                    for (int n = 0; n < maxLen; n++) {
                        double b = (double) n / maxLen * 100;
                        int progress = (int) b + 1;
                        setProgress(progress);
                        int m = n + 1;

                        if (trimmer.getLenMap().containsKey(m)) {
                            long y = trimmer.getLenMap().get(m);
                            total += y;
                            xySeries.add(m, y);

                        }

                        double min = dist.get(m).get("min");
                        double q1 = dist.get(m).get("q1");
                        double mean = dist.get(m).get("mean");
                        double median = dist.get(m).get("median");
                        double q3 = dist.get(m).get("q3");
                        double max = dist.get(m).get("max");

                        series.add((double) m, median, min, max);

                        if (item != null) {
                            double prevMin = item.getMinRegularValue().doubleValue();
                            double prevQ1 = item.getQ1().doubleValue();
                            double prevMean = item.getMean().doubleValue();
                            double prevMedian = item.getMedian().doubleValue();
                            double prevQ3 = item.getQ3().doubleValue();
                            double prevMax = item.getMaxRegularValue().doubleValue();

//                        double diffMin = Math.abs(min - prevMin);
                            double diffQ1 = Math.abs(q1 - prevQ1);
//                        double diffMean = Math.abs(mean - prevMean);
                            double diffMedian = Math.abs(median - prevMedian);
                            double diffQ3 = Math.abs(q3 - prevQ3);
//                        double diffMax = Math.abs(max - prevMax);

                            final int cutOff = 1;
                            if ( //                                diffMin < cutOff && 
                                    diffQ1 < cutOff
                                    && //                                diffMean < cutOff && 
                                    diffMedian < cutOff
                                    && diffQ3 < cutOff //                                && diffMax < cutOff
                                    ) {
                                double minMean = (min + prevMin) / 2;
                                double q1Mean = (q1 + prevQ1) / 2;
                                double meanMean = (mean + prevMean) / 2;
                                double medianMean = (median + prevMedian) / 2;
                                double q3Mean = (q3 + prevQ3) / 2;
                                double maxMean = (max + prevMax) / 2;

                                item = new BoxAndWhiskerItem(meanMean, medianMean, q1Mean, q3Mean, minMean, maxMean, null, null, null);
                            } else {
                                item = new BoxAndWhiskerItem(mean, median, q1, q3, min, max, null, null, null);
                                dataset.add(item, "Base", m);
                            }
                        } else {
                            item = new BoxAndWhiskerItem(mean, median, q1, q3, min, max, null, null, null);
                            dataset.add(item, "Base", m);
                        }
                    }

                    String description = "Total: " + total;
                    xySeries.setKey(description);
                    xyCollection.addSeries(xySeries);
                    collection.addSeries(series);
                    xyBarPlot.setDataset(xyCollection);
                    xyPlot.setDataset(collection);
                    cPlot.setDataset(dataset);

                    JFreeChart chart = new JFreeChart(cPlot);
                    JFreeChart devChart = new JFreeChart(xyPlot);
                    JFreeChart xyBarChart = new JFreeChart(xyBarPlot);

                    devChart.removeLegend();
                    chart.removeLegend();

                    boxPanel = new ChartPanel(chart);
                    devPanel = new ChartPanel(devChart);
                    lenPanel = new ChartPanel(xyBarChart);

                    plotPanel1.removeAll();

                    boxPanel.setSize(plotPanel1.getSize());
                    devPanel.setSize(plotPanel1.getSize());
                    lenPanel.setSize(plotPanel1.getSize());

                    if (lenDistr.isSelected()) {
                        plotPanel1.add(lenPanel);
                    } else if (qualDistr.isSelected()) {
                        if (boxRadio.isSelected()) {
                            plotPanel1.add(boxPanel);
                        } else if (devRadio.isSelected()) {
                            plotPanel1.add(devPanel);
                        }
                    }

                    plotPanel1.getParent().validate();
                    plotPanel1.repaint();
                    plotFrame.setVisible(true);
                    plotPanel1.setVisible(true);
                    plotted = true;
                }

                if (trimPlotted == false && output != null) {

                    DeviationRenderer devRender = new DeviationRenderer(true, false);
                    devRender.setSeriesStroke(0, new BasicStroke(2F, 1, 1));
                    devRender.setSeriesFillPaint(0, new Color(58, 95, 205));
                    devRender.setSeriesPaint(0, new Color(77, 77, 255));

                    BoxAndWhiskerRenderer render = new BoxAndWhiskerRenderer();
                    render.setMeanVisible(false);
                    render.setFillBox(true);
                    render.setSeriesPaint(0, Color.BLUE);
                    render.setUseOutlinePaintForWhiskers(false);
                    render.setUseOutlinePaintForWhiskers(true);
                    render.setBaseOutlinePaint(Color.BLACK);

                    XYBarRenderer barRender = new XYBarRenderer(0.0);
                    barRender.setShadowVisible(false);
                    barRender.setSeriesPaint(0, Color.BLUE);

                    CategoryAxis axisX = new CategoryAxis();
                    axisX.setTickLabelsVisible(false);
                    NumberAxis axisY = new NumberAxis();
                    NumberAxis numAxisX = new NumberAxis();
                    NumberAxis lenAxisX = new NumberAxis();
                    NumberAxis lenAxisY = new NumberAxis();

                    IntervalMarker markerBAD = new IntervalMarker(0, 18, Color.getHSBColor(1.0f, 0.5f, 1.0f));
                    IntervalMarker markerNEUTRAL = new IntervalMarker(18, 30, Color.getHSBColor(0.1f, 0.5f, 1.0f));
                    IntervalMarker markerGOOD = new IntervalMarker(30, 70, Color.getHSBColor(0.3f, 0.5f, 1.0f));

                    CategoryPlot cPlot = new CategoryPlot();
                    cPlot.setRangeAxis(axisY);
                    cPlot.setDomainAxis(axisX);
                    cPlot.setRenderer(render);
                    cPlot.addRangeMarker(markerBAD, Layer.BACKGROUND);
                    cPlot.addRangeMarker(markerNEUTRAL, Layer.BACKGROUND);
                    cPlot.addRangeMarker(markerGOOD, Layer.BACKGROUND);

                    XYPlot xyPlot = new XYPlot();
                    xyPlot.setRangeAxis(axisY);
                    xyPlot.setDomainAxis(numAxisX);
                    xyPlot.setRenderer(devRender);
                    xyPlot.addRangeMarker(markerBAD, Layer.BACKGROUND);
                    xyPlot.addRangeMarker(markerNEUTRAL, Layer.BACKGROUND);
                    xyPlot.addRangeMarker(markerGOOD, Layer.BACKGROUND);

                    XYPlot xyBarPlot = new XYPlot();
                    xyBarPlot.setDomainAxis(lenAxisX);
                    xyBarPlot.setRangeAxis(lenAxisY);
                    xyBarPlot.setRenderer(barRender);
                    xyBarPlot.setRangeGridlinePaint(Color.DARK_GRAY);

                    progBars.setIndeterminate(true);
                    progBars.setStringPainted(true);

                    if (outStats == null) {
                        progBars.setString("Analyzing trimmed file...");
                        outTrimmer.analyze();
                        outStats = outTrimmer.getStat();
                        setProperty(outStats);
                    }

                    DoubleArrayList lenList = new DoubleArrayList();
                    for (Integer x : outTrimmer.getLenMap().keySet()) {
                        lenList.add(x);
                    }

                    progBars.setString("Begin deep analysis of trimmed file for plotting...");
                    Map<Integer, Map<String, Double>> dist = outTrimmer.getDistribution();
                    progBars.setIndeterminate(false);
                    progBars.setStringPainted(false);
                    setProgress(0);
                    final int maxLen = (int) Descriptive.max(lenList);
                    DefaultBoxAndWhiskerCategoryDataset dataset =
                            new DefaultBoxAndWhiskerCategoryDataset();
                    YIntervalSeriesCollection collection = new YIntervalSeriesCollection();
                    XYSeriesCollection xyCollection = new XYSeriesCollection();
                    XYSeries xySeries = new XYSeries("Length");
                    BoxAndWhiskerItem item = null;
                    YIntervalSeries series = new YIntervalSeries("Bases");
                    long total = 0;
                    for (int n = 0; n < maxLen; n++) {
                        double b = (double) n / maxLen * 100;
                        int progress = (int) b + 1;
                        setProgress(progress);
                        int m = n + 1;

                        if (outTrimmer.getLenMap().containsKey(m)) {
                            long y = outTrimmer.getLenMap().get(m);
                            total += y;
                            xySeries.add(m, y);
                        }

                        double min = dist.get(m).get("min");
                        double q1 = dist.get(m).get("q1");
                        double mean = dist.get(m).get("mean");
                        double median = dist.get(m).get("median");
                        double q3 = dist.get(m).get("q3");
                        double max = dist.get(m).get("max");

                        series.add((double) m, median, min, max);

                        if (item != null) {
                            double prevMin = item.getMinRegularValue().doubleValue();
                            double prevQ1 = item.getQ1().doubleValue();
                            double prevMean = item.getMean().doubleValue();
                            double prevMedian = item.getMedian().doubleValue();
                            double prevQ3 = item.getQ3().doubleValue();
                            double prevMax = item.getMaxRegularValue().doubleValue();

//                        double diffMin = Math.abs(min - prevMin);
                            double diffQ1 = Math.abs(q1 - prevQ1);
//                        double diffMean = Math.abs(mean - prevMean);
                            double diffMedian = Math.abs(median - prevMedian);
                            double diffQ3 = Math.abs(q3 - prevQ3);
//                        double diffMax = Math.abs(max - prevMax);

                            final int cutOff = 1;
                            if ( //                                diffMin < cutOff && 
                                    diffQ1 < cutOff
                                    && //                                diffMean < cutOff && 
                                    diffMedian < cutOff
                                    && diffQ3 < cutOff //                                && diffMax < cutOff
                                    ) {
                                double minMean = (min + prevMin) / 2;
                                double q1Mean = (q1 + prevQ1) / 2;
                                double meanMean = (mean + prevMean) / 2;
                                double medianMean = (median + prevMedian) / 2;
                                double q3Mean = (q3 + prevQ3) / 2;
                                double maxMean = (max + prevMax) / 2;

                                item = new BoxAndWhiskerItem(meanMean, medianMean, q1Mean, q3Mean, minMean, maxMean, null, null, null);
                            } else {
                                item = new BoxAndWhiskerItem(mean, median, q1, q3, min, max, null, null, null);
                                dataset.add(item, "Base", m);
                            }
                        } else {
                            item = new BoxAndWhiskerItem(mean, median, q1, q3, min, max, null, null, null);
                            dataset.add(item, "Base", m);
                        }
                    }

                    String description = "Total: " + total;
                    xySeries.setKey(description);
                    xyCollection.addSeries(xySeries);
                    xyBarPlot.setDataset(xyCollection);
                    collection.addSeries(series);
                    cPlot.setDataset(dataset);
                    xyPlot.setDataset(collection);

                    JFreeChart chart = new JFreeChart(cPlot);
                    JFreeChart devChart = new JFreeChart(xyPlot);
                    JFreeChart xyBarChart = new JFreeChart(xyBarPlot);
                    devChart.removeLegend();
                    chart.removeLegend();

                    boxPanelTrimmed = new ChartPanel(chart);
                    devPanelTrimmed = new ChartPanel(devChart);
                    lenPanelTrimmed = new ChartPanel(xyBarChart);

                    plotPanelTrimmed1.removeAll();
                    boxPanelTrimmed.setSize(plotPanelTrimmed1.getSize());
                    devPanelTrimmed.setSize(plotPanelTrimmed1.getSize());
                    lenPanelTrimmed.setSize(plotPanelTrimmed1.getSize());

                    if (lenDistr.isSelected()) {
                        plotPanelTrimmed1.add(lenPanelTrimmed);
                    } else if (qualDistr.isSelected()) {
                        if (boxRadio.isSelected()) {
                            plotPanelTrimmed1.add(boxPanelTrimmed);
                        } else if (devRadio.isSelected()) {
                            plotPanelTrimmed1.add(devPanelTrimmed);
                        }
                    }

                    plotPanelTrimmed1.getParent().validate();
                    plotPanelTrimmed1.repaint();
                    plotFrame.setVisible(true);
                    plotPanelTrimmed1.setVisible(true);
                    trimPlotted = true;
                }

                lenDistr.setEnabled(true);
                qualDistr.setEnabled(true);
                boxRadio.setEnabled(true);
                devRadio.setEnabled(true);
                enableButtons(true);
                return null;
            }
        };

        plotting.addPropertyChangeListener(listener);
        plotting.execute();
    }//GEN-LAST:event_plotButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;






                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(StreamingTrimGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(StreamingTrimGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(StreamingTrimGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(StreamingTrimGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        Toolkit defToolkit = Toolkit.getDefaultToolkit();
        Dimension screen = defToolkit.getScreenSize();
        final int x = (screen.width * 20) / 100;
        final int y = (screen.height * 20) / 100;


        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {

                StreamingTrimGUI str = new StreamingTrimGUI();
                str.setLocation(x, y);
                str.setVisible(true);

//                new StreamingTrimGUI().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem advOption;
    private javax.swing.JFrame advancedOption;
    private javax.swing.JButton analyseButton;
    private javax.swing.JRadioButton boxRadio;
    private javax.swing.JCheckBox checkCuToFF;
    private javax.swing.JCheckBox checkMinLen;
    private javax.swing.JCheckBox checkOffSet;
    private javax.swing.JButton convert;
    private javax.swing.JPanel cutPanel;
    private javax.swing.JRadioButton devRadio;
    private javax.swing.JOptionPane exception;
    private javax.swing.JMenuItem exitMenu;
    private javax.swing.JOptionPane fileAlredyExist;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JMenu fileMenu;
    private javax.swing.ButtonGroup functionGroup;
    private javax.swing.JRadioButton guessButton;
    private javax.swing.JRadioButton illuminaButton;
    private javax.swing.JFrame infoFrame;
    private javax.swing.JMenu infoMenu;
    private javax.swing.JScrollPane infoScroll;
    private javax.swing.JPanel inputFilePanel;
    private javax.swing.JLabel inputLabel;
    private javax.swing.JTextField insCutOff;
    private javax.swing.JTextField insMinLeng;
    private javax.swing.JTextField insOffSet;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JRadioButton lenDistr;
    private javax.swing.JLabel lengthLab;
    private javax.swing.JLabel mean;
    private javax.swing.JLabel meanLen;
    private javax.swing.JLabel meanQual;
    private javax.swing.JMenuItem openItem;
    private javax.swing.JButton plotButton;
    private javax.swing.JFrame plotFrame;
    private javax.swing.JMenu plotMenu;
    private javax.swing.JPanel plotOptionPanel;
    private javax.swing.JPanel plotPanel;
    private javax.swing.JPanel plotPanel1;
    private javax.swing.JPanel plotPanelTrimmed;
    private javax.swing.JPanel plotPanelTrimmed1;
    private javax.swing.ButtonGroup plotTypeGroup;
    private javax.swing.JMenuItem pltWin;
    private javax.swing.JProgressBar progBars;
    private javax.swing.ButtonGroup propGroup;
    private javax.swing.JRadioButton propInput;
    private javax.swing.JRadioButton propTrim;
    private javax.swing.JRadioButton qualDistr;
    private javax.swing.JLabel qualLab;
    private javax.swing.ButtonGroup readsTyp;
    private javax.swing.JPanel readsTypePanel;
    private javax.swing.JRadioButton sangerButton;
    private javax.swing.JRadioButton solexaButton;
    private javax.swing.JLabel stDev;
    private javax.swing.JLabel stLen;
    private javax.swing.JLabel stQual;
    private javax.swing.JTextPane textInfo;
    private javax.swing.JCheckBox trimToFasta;
    private javax.swing.JButton trimmButton;
    private javax.swing.ButtonGroup typePlotGroup;
    // End of variables declaration//GEN-END:variables
}
