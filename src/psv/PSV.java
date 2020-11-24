package psv;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.math.NumberUtils;
import parser.PrismParser;
import parser.ast.*;
import prism.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PSV {
    private final PrismLog mainLog = new PrismFileLog("stdout");
    private final Prism prism = new Prism(mainLog);
    private SyncSwarmFile syncSwarmFile;
    private AsyncSwarmFile asyncSwarmFile;
    private FaultFile faultFile;
    private PropertiesFile pf;
    private final IndexCalculator indexCalculator = new IndexCalculator();

    public static void main(final String[] args) {
        final Options options = new Options();

        final Option mode = new Option("c", "concrete", true,
                "Pass a comma separated list of integers to check a concrete system of that size.");
        options.addOption(mode);

        final Option async = new Option("a", "async", false,
                "Check an asynchronous system (default is synchronous).");
        options.addOption(async);

        final Option model = new Option("s", "swarmFile", true, "Path to the swarm model file.");
        model.setRequired(true);
        options.addOption(model);

        final Option properties = new Option("p", "propertiesFile", true, "Path to properties file.");
        properties.setRequired(true);
        options.addOption(properties);

        final Option faults = new Option("f", "faultsFile", true, "Path to a file specifying faults to inject.");
        options.addOption(faults);

        final Option export = new Option("e", "export", true, "Path to export DOT file of model to.");
        options.addOption(export);

        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (final ParseException e) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("psv", options);
            System.exit(1);
            throw new IllegalStateException("Parsing arguments failed.");
        }

        try {
            new PSV().go(cmd);
        } catch (final PrismException e) {
            throw new IllegalStateException(e);
        }
    }

    private void go(final CommandLine cmd) throws PrismException {
        prism.initialise();
        prism.setEngine(Prism.EXPLICIT);
        if (cmd.hasOption("f")) {
            if (!cmd.hasOption("a"))
                throw new IllegalStateException("Fault injection is only supported on asynchronous models.");
            readInFaultFile(cmd.getOptionValue("f"));
        }
        if (cmd.hasOption("a"))
            readInAsyncSwarmFile(cmd.getOptionValue("s"));
        else
            readInSyncSwarmFile(cmd.getOptionValue("s"));
        readInPropertiesFile(cmd.getOptionValue("p"));
        final ModelGenerator modelGenerator;
        if (cmd.hasOption("c")) {
            final List<Integer> numAgents = new ArrayList<>();
            for (final String n : cmd.getOptionValue("c").split(","))
                numAgents.add(NumberUtils.createInteger(n));
            if (cmd.hasOption("f"))
                modelGenerator = new FaultyConcreteModelGenerator(asyncSwarmFile, faultFile, numAgents);
            else if (cmd.hasOption("a"))
                modelGenerator = new AsyncConcreteModelGenerator(asyncSwarmFile, numAgents);
            else
                modelGenerator = new SyncConcreteModelGenerator(syncSwarmFile, numAgents);
        } else {
            if (cmd.hasOption("a")) {
                indexCalculator.fillTo(asyncSwarmFile.getAgents().size());
                processLabelList(asyncSwarmFile.getLabelList());
            } else {
                indexCalculator.fillTo(syncSwarmFile.getAgents().size());
                processLabelList(syncSwarmFile.getLabelList());
            }
            processLabelList(pf.getLabelList());
            for (int i = 0; i < pf.getNumProperties(); ++i)
                pf.getPropertyObject(i).getExpression().accept(indexCalculator);
            if (cmd.hasOption("f"))
                modelGenerator = new FaultyAbstractModelGenerator(asyncSwarmFile, faultFile, indexCalculator.getIndex());
            else if (cmd.hasOption("a"))
                modelGenerator = new AsyncAbstractModelGenerator(asyncSwarmFile, indexCalculator.getIndex());
            else
                modelGenerator = new SyncAbstractModelGenerator(syncSwarmFile, indexCalculator.getIndex());
            indexCalculator.reset();
        }
        prism.loadModelGenerator(modelGenerator);
        prism.buildModelIfRequired();
        if (cmd.hasOption("e"))
            prism.getBuiltModelExplicit().exportToDotFile(cmd.getOptionValue("e"));
        for (int i = 0; i < pf.getNumProperties(); ++i)
            prism.modelCheck(pf, pf.getProperty(i));
    }

    private void readInAsyncSwarmFile(final String fileName) throws PrismLangException {
        final PrismParser parser;
        try {
            parser = Prism.getPrismParser();
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }
        try (final FileInputStream fis = new FileInputStream(new File(fileName))) {
            asyncSwarmFile = parser.parseAsyncSwarmFile(fis);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        } finally {
            Prism.releasePrismParser();
        }
    }

    private void readInFaultFile(final String fileName) throws PrismLangException {
        final PrismParser parser;
        try {
            parser = Prism.getPrismParser();
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }
        try (final FileInputStream fis = new FileInputStream(new File(fileName))) {
            faultFile = parser.parseFaultFile(fis);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        } finally {
            Prism.releasePrismParser();
        }
    }

    private void readInSyncSwarmFile(final String fileName) throws PrismLangException {
        final PrismParser parser;
        try {
            parser = Prism.getPrismParser();
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }
        try (final FileInputStream fis = new FileInputStream(new File(fileName))) {
            syncSwarmFile = parser.parseSyncSwarmFile(fis);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        } finally {
            Prism.releasePrismParser();
        }
    }

    private void readInPropertiesFile(final String fileName) throws PrismLangException {
        final PrismParser parser;
        try {
            parser = Prism.getPrismParser();
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }
        try (final FileInputStream fis = new FileInputStream(new File(fileName))) {
            pf = parser.parsePropertiesFile(null, fis);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        } finally {
            Prism.releasePrismParser();
        }
    }

    private void processLabelList(final LabelList labelList) throws PrismLangException {
        for (int i = 0; i < labelList.size(); ++i) {
            labelList.getLabel(i).accept(indexCalculator);
            indexCalculator.endVisitLabel(labelList.getLabelName(i));
        }
    }
}
