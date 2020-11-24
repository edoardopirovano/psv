package psv;

import org.apache.commons.cli.*;
import parser.PrismParser;
import parser.ast.LabelList;
import parser.ast.PropertiesFile;
import parser.ast.SwarmFile;
import prism.*;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class PSV {
    private PrismLog mainLog = new PrismFileLog("stdout");
    private Prism prism = new Prism(mainLog);
    private SwarmFile sf;
    private PropertiesFile pf;
    private ModelGenerator modelGenerator;
    private IndexCalculator indexCalculator = new IndexCalculator();

    public static void main(String[] args) {
        Options options = new Options();

        Option mode = new Option("c", "concrete", true,
                "Pass an integer to check a concrete system of that size.");
        options.addOption(mode);

        Option model = new Option("s", "swarmFile", true, "Path to the swarm model file.");
        model.setRequired(true);
        options.addOption(model);

        Option properties = new Option("p", "propertiesFile", true, "Path to properties file.");
        properties.setRequired(true);
        options.addOption(properties);

        Option export = new Option("e", "export", true, "Path to export DOT file of model to.");
        options.addOption(export);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "psv", options );
            System.exit(1);
            throw new IllegalStateException("Parsing arguments failed.");
        }

        try {
            new PSV().go(cmd);
        } catch (PrismException e) {
            throw new IllegalStateException(e);
        }
    }

    private void go(CommandLine cmd) throws PrismException {
        prism.initialise();
        prism.setEngine(Prism.EXPLICIT);
        readInSwarmFile(cmd.getOptionValue("s"));
        readInPropertiesFile(cmd.getOptionValue("p"));
        if (cmd.hasOption("c"))
            modelGenerator = new ConcreteModelGenerator(sf, NumberUtils.createInteger(cmd.getOptionValue("c")));
        else
            createAbstractModelGenerator();
        prism.loadModelGenerator(modelGenerator);
        prism.buildModelIfRequired();
        if (cmd.hasOption("e"))
            prism.getBuiltModelExplicit().exportToDotFile(cmd.getOptionValue("e"));
        for (int i = 0; i < pf.getNumProperties(); ++i)
            prism.modelCheck(pf, pf.getProperty(i));
    }

    private void readInSwarmFile(String fileName) throws PrismLangException {
        PrismParser parser;
        try {
            parser = Prism.getPrismParser();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        try (FileInputStream fis = new FileInputStream(new File(fileName))) {
            sf = parser.parseSwarmFile(fis);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            Prism.releasePrismParser();
        }
    }

    private void readInPropertiesFile(String fileName) throws PrismLangException {
        PrismParser parser;
        try {
            parser = Prism.getPrismParser();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        try (FileInputStream fis = new FileInputStream(new File(fileName))) {
            pf = parser.parsePropertiesFile(null, fis);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            Prism.releasePrismParser();
        }
    }

    private void createAbstractModelGenerator() throws PrismException {
        processLabelList(sf.getLabelList());
        processLabelList(pf.getLabelList());
        for (int i = 0; i < pf.getNumProperties(); ++i)
            pf.getPropertyObject(i).getExpression().accept(indexCalculator);
        modelGenerator = new AbstractModelGenerator(sf, indexCalculator.getIndex());
        indexCalculator.reset();
    }

    private void processLabelList(LabelList labelList) throws PrismLangException {
        for (int i = 0; i < labelList.size(); ++i) {
            labelList.getLabel(i).accept(indexCalculator);
            indexCalculator.endVisitLabel(labelList.getLabelName(i));
        }
    }
}
