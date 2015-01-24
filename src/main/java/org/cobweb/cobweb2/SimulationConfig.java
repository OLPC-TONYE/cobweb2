package org.cobweb.cobweb2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.cobweb.cobweb2.core.NullPhenotype;
import org.cobweb.cobweb2.core.Phenotype;
import org.cobweb.cobweb2.impl.AgentFoodCountable;
import org.cobweb.cobweb2.impl.ComplexAgent;
import org.cobweb.cobweb2.impl.ComplexAgentParams;
import org.cobweb.cobweb2.impl.ComplexEnvironment;
import org.cobweb.cobweb2.impl.ComplexEnvironmentParams;
import org.cobweb.cobweb2.impl.ControllerParams;
import org.cobweb.cobweb2.impl.FieldPhenotype;
import org.cobweb.cobweb2.impl.SimulationParams;
import org.cobweb.cobweb2.impl.ai.GeneticController;
import org.cobweb.cobweb2.impl.ai.GeneticControllerParams;
import org.cobweb.cobweb2.impl.learning.ComplexAgentLearning;
import org.cobweb.cobweb2.impl.learning.LearningParams;
import org.cobweb.cobweb2.plugins.abiotic.TemperatureParams;
import org.cobweb.cobweb2.plugins.disease.DiseaseParams;
import org.cobweb.cobweb2.plugins.food.ComplexFoodParams;
import org.cobweb.cobweb2.plugins.genetics.GeneticParams;
import org.cobweb.cobweb2.plugins.production.ProductionParams;
import org.cobweb.cobweb2.ui.compatibility.ConfigUpgrader;
import org.cobweb.io.ChoiceCatalog;
import org.cobweb.io.ParameterSerializer;
import org.cobweb.util.Versionator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Used to organize, modify, and access simulation parameters.
 */
public class SimulationConfig implements SimulationParams {
	private static void removeIgnorableWSNodes(Element parent) {
		Node nextNode = parent.getFirstChild();
		for (Node child = parent.getFirstChild(); nextNode != null;) {
			child = nextNode;
			nextNode = child.getNextSibling();
			if (child.getNodeType() == Node.TEXT_NODE) {
				// Checks if the text node is ignorable
				if (child.getTextContent().matches("^\\s*$")) {
					parent.removeChild(child);
				}
			} else if (child.getNodeType() == Node.ELEMENT_NODE) {
				removeIgnorableWSNodes((Element )child);
			}
		}
	}

	public final ParameterSerializer serializer;

	private String fileName = null;

	private ComplexEnvironmentParams envParams;

	private GeneticParams geneticParams;

	private ComplexAgentParams[] agentParams;

	private ProductionParams[] prodParams;

	private LearningParams learningParams;

	private ComplexFoodParams[] foodParams;

	private DiseaseParams[] diseaseParams;

	private TemperatureParams tempParams;

	private ControllerParams controllerParams;

	public final ChoiceCatalog choiceCatalog;

	/**
	 * Creates the default Cobweb simulation parameters.
	 */
	public SimulationConfig() {
		choiceCatalog = new ChoiceCatalog();
		choiceCatalog.addChoice(Phenotype.class, new NullPhenotype());
		for(Phenotype x : FieldPhenotype.getPossibleValues()) {
			choiceCatalog.addChoice(Phenotype.class, x);
		}

		serializer = new ParameterSerializer(choiceCatalog);

		envParams = new ComplexEnvironmentParams();
		setDefaultClassReferences();

		agentParams = new ComplexAgentParams[envParams.getAgentTypes()];
		for (int i = 0; i < envParams.getAgentTypes(); i++) {
			agentParams[i] = new ComplexAgentParams(envParams);
			agentParams[i].type = i;
		}

		foodParams = new ComplexFoodParams[envParams.getFoodTypes()];
		for (int i = 0; i < envParams.getFoodTypes(); i++) {
			foodParams[i] = new ComplexFoodParams();
			foodParams[i].type = i;
		}

		geneticParams = new GeneticParams(envParams);

		diseaseParams = new DiseaseParams[envParams.getAgentTypes()];
		for (int i = 0; i < envParams.getAgentTypes(); i++) {
			diseaseParams[i] = new DiseaseParams(envParams);
		}

		prodParams = new ProductionParams[envParams.getAgentTypes()];
		for (int i = 0; i < envParams.getAgentTypes(); i++) {
			prodParams[i] = new ProductionParams();
			prodParams[i].type = i;
		}

		tempParams = new TemperatureParams(envParams);

		learningParams = new LearningParams(envParams);

		controllerParams = new GeneticControllerParams(this);

		fileName = "default simulation";
	}

	protected void setDefaultClassReferences() {
		envParams.controllerName = GeneticController.class.getName();
		envParams.agentName = ComplexAgent.class.getName();
		envParams.environmentName = ComplexEnvironment.class.getName();
	}

	/**
	 * Constructor that allows input from a file stream to configure simulation parameters.
	 *
	 * @param file Input file stream.
	 */
	public SimulationConfig(InputStream file) {
		this();
		this.fileName = ":STREAM:" + file.toString() + ":";
		loadFile(file);
	}

	/**
	 * Constructor that allows input from a file to configure the simulation parameters.
	 *
	 * @param fileName Name of the file used for simulation configuration.
	 * @see SimulationConfig#loadFile(InputStream)
	 */
	public SimulationConfig(String fileName) throws FileNotFoundException {
		this();
		this.fileName = fileName;
		File file = new File(fileName);
		ConfigUpgrader.upgradeConfigFile(file);
		FileInputStream configStream = new FileInputStream(file);
		loadFile(configStream);
		try {
			configStream.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * @return Agent parameters
	 */
	public ComplexAgentParams[] getAgentParams() {
		return agentParams;
	}

	public ProductionParams[] getProdParams() {
		return prodParams;
	}


	/**
	 * @return Disease parameters
	 */
	public DiseaseParams[] getDiseaseParams() {
		return diseaseParams;
	}

	/**
	 * @return Environment parameters
	 */
	public ComplexEnvironmentParams getEnvParams() {
		return envParams;
	}

	/**
	 * @return Simulation configuration file name
	 */
	public String getFilename() {
		return fileName;
	}

	/**
	 * @return Food parameters
	 */
	public ComplexFoodParams[] getFoodParams() {
		return foodParams;
	}

	/**
	 * @return Genetic parameters
	 */
	public GeneticParams getGeneticParams() {
		return geneticParams;
	}

	/**
	 * @return Temperature parameters
	 */
	public TemperatureParams getTempParams() {
		return tempParams;
	}

	public LearningParams getLearningParams() {
		return learningParams;
	}


	/**
	 * This method extracts data from the simulation configuration file and
	 * loads the data into the simulation parameters.  It does this by first
	 * creating a tree that holds all data from file using the DocumentBuilder
	 * class.  Next, the root node of the tree is passed to the
	 * AbstractReflectionParams.loadConfig(Node) method for processing.  This
	 * processing allows the ConfXMLTags to overwrite the default parameters
	 * used when constructing Cobweb environment parameters.
	 *
	 * <p>Once the environment parameters have been extracted successfully,
	 * the rest of the Cobweb parameters can be set (temperature, genetics,
	 * agents, etc.) using the environment parameters.
	 *
	 * @param file The current simulation configuration file.
	 * @see javax.xml.parsers.DocumentBuilder
	 * @throws IllegalArgumentException Unable to open the simulation configuration file.
	 */
	private void loadFile(InputStream file) throws IllegalArgumentException {
		// read these variables from the xml file

		// DOM initialization
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringElementContentWhitespace(true);
		factory.setIgnoringComments(true);
		// factory.setValidating(true);

		Document document;
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(file);
		} catch (SAXException ex) {
			throw new IllegalArgumentException("Can't open config file", ex);
		} catch (ParserConfigurationException ex) {
			throw new IllegalArgumentException("Can't open config file", ex);
		} catch (IOException ex) {
			throw new IllegalArgumentException("Can't open config file", ex);
		}


		Node root = document.getFirstChild();
		removeIgnorableWSNodes((Element) root);

		envParams = new ComplexEnvironmentParams();
		setDefaultClassReferences();

		serializer.load(envParams, root);

		ConfigUpgrader.upgrade(envParams);

		agentParams = new ComplexAgentParams[envParams.getAgentTypes()];
		prodParams = new ProductionParams[envParams.getAgentTypes()];
		foodParams = new ComplexFoodParams[envParams.getFoodTypes()];
		diseaseParams = new DiseaseParams[envParams.getAgentTypes()];

		for (int i = 0; i < envParams.getAgentTypes(); i++)
			diseaseParams[i] = new DiseaseParams(envParams);

		tempParams = new TemperatureParams(envParams);

		learningParams = new LearningParams(envParams);

		geneticParams = new GeneticParams(envParams);

		NodeList nodes = root.getChildNodes();
		int agent = 0;
		int prod = 0;
		int food = 0;
		for (int j = 0; j < nodes.getLength(); j++) {
			Node node = nodes.item(j);
			String nodeName = node.getNodeName();

			if (nodeName.equals("ga")) {
				serializer.load(geneticParams, node);

			} else if (nodeName.equals("agent")) {
				ComplexAgentParams p = new ComplexAgentParams(envParams);
				serializer.load(p, node);
				if (p.type < 0)
					p.type = agent++;
				if (p.type >= envParams.getAgentTypes())
					continue;
				agentParams[p.type] = p;
			} else if (nodeName.equals("production")) {
				ProductionParams p = new ProductionParams();
				serializer.load(p, node);
				if (p.type < 0)
					p.type = prod++;
				if (p.type >= envParams.getAgentTypes())
					continue;
				prodParams[p.type] = p;
			} else if (nodeName.equals("food")) {
				ComplexFoodParams p = new ComplexFoodParams();
				serializer.load(p, node);
				if (p.type < 0)
					p.type = food++;

				if (p.type >= envParams.getFoodTypes())
					continue;

				foodParams[p.type] = p;
			} else if (nodeName.equals("disease")) {
				parseDiseaseParams(node);
			} else if (nodeName.equals("Temperature")) {
				serializer.load(tempParams, node);
			} else if (nodeName.equals("Learning")) {
				serializer.load(learningParams, node);
			} else if (nodeName.equals("ControllerConfig")){
				// FIXME: this is initialized after everything else because
				// Controllers use SimulationParams.getPluginParameters()
				// and things like disease provide are those plugins
				try {
					controllerParams = (ControllerParams) Class.forName(envParams.controllerName + "Params")
							.getConstructor(SimulationParams.class)
							.newInstance((SimulationParams) this);
					controllerParams.resize(envParams);
				} catch (InstantiationError | ClassNotFoundException | NoSuchMethodException |
						InstantiationException | IllegalAccessException | InvocationTargetException ex) {
					throw new RuntimeException("Could not set up controller", ex);
				}
				serializer.load(controllerParams, node);
			}
		}
		for (int i = 0; i < agentParams.length; i++) {
			if (agentParams[i] == null) {
				agentParams[i] = new ComplexAgentParams(envParams);
				agentParams[i].type = i;
			}
		}
		for (int i = 0; i < prodParams.length; i++) {
			if (prodParams[i] == null) {
				prodParams[i] = new ProductionParams();
				prodParams[i].type = i;
			}
		}
		for (int i = 0; i < foodParams.length; i++) {
			if (foodParams[i] == null) {
				foodParams[i] = new ComplexFoodParams();
				foodParams[i].type = i;
			}
		}

	}

	private void parseDiseaseParams(Node root) {
		NodeList nodes = root.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			if (i >= envParams.getAgentTypes())
				break;
			DiseaseParams dp = new DiseaseParams(envParams);
			serializer.load(dp, n);
			diseaseParams[i] = dp;
		}
		for (int i = 0; i < diseaseParams.length; i++) {
			if (diseaseParams[i] == null)
				diseaseParams[i] = new DiseaseParams(envParams);
		}
	}

	/**
	 * Writes the information stored in this tree to an XML file, conforming to the rules of our spec.
	 *
	 */
	public void write(OutputStream stream) {
		Document d;
		try {
			d = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException ex) {
			throw new RuntimeException(ex);
		}
		Element root = d.createElementNS("http://cobweb.ca/schema/cobweb2/config", "COBWEB2Config");
		root.setAttribute("config-version", "2015-01-14");
		root.setAttribute("cobweb-version", Versionator.getVersion());

		serializer.save(envParams, root, d);
		for (int i = 0; i < envParams.getAgentTypes(); i++) {
			Element node = d.createElement("agent");
			serializer.save(agentParams[i], node, d);
			root.appendChild(node);
		}

		for (int i = 0; i < envParams.getAgentTypes(); i++) {
			Element node = d.createElement("production");
			serializer.save(prodParams[i], node, d);
			root.appendChild(node);
		}

		for (int i = 0; i < envParams.getFoodTypes(); i++) {
			Element node = d.createElement("food");
			serializer.save(foodParams[i], node, d);
			root.appendChild(node);
		}

		Element ga = d.createElement("ga");
		serializer.save(geneticParams, ga, d);

		root.appendChild(ga);

		Node disease = d.createElement("disease");
		for (DiseaseParams diseaseParam : diseaseParams) {
			Element node = d.createElement("agent");
			serializer.save(diseaseParam, node, d);
			disease.appendChild(node);
		}
		root.appendChild(disease);

		Element temp = d.createElement("Temperature");
		serializer.save(tempParams, temp, d);
		root.appendChild(temp);

		if (this.envParams.agentName.equals(ComplexAgentLearning.class.getName())) {
			Element learn = d.createElement("Learning");
			serializer.save(learningParams, learn, d);
			root.appendChild(learn);
		}

		Element controller = d.createElement("ControllerConfig");
		serializer.save(controllerParams, controller, d);
		root.appendChild(controller);

		d.appendChild(root);

		Source s = new DOMSource(d);

		Transformer t;
		TransformerFactory tf = TransformerFactory.newInstance();
		try {
			t = tf.newTransformer();

		} catch (TransformerConfigurationException ex) {
			throw new RuntimeException(ex);
		}
		t.setOutputProperty(OutputKeys.INDENT, "yes");
		t.setOutputProperty(OutputKeys.STANDALONE, "yes");
		t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

		Result r = new StreamResult(stream);
		try {
			t.transform(s, r);
		} catch (TransformerException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void SetAgentTypeCount(int count) {
		this.envParams.agentTypeCount = count;
		this.envParams.foodTypeCount = count;

		{
			ComplexAgentParams[] n = Arrays.copyOf(this.agentParams, count);
			for (int i = 0; i < this.agentParams.length && i < count; i++) {
				n[i].resize(envParams);
			}
			for (int i = this.agentParams.length; i < count; i++) {
				n[i] = new ComplexAgentParams(envParams);
			}
			this.agentParams = n;
		}

		{
			ComplexFoodParams[] n = Arrays.copyOf(this.foodParams, count);
			for (int i = this.foodParams.length; i < count; i++) {
				n[i] = new ComplexFoodParams();
			}
			this.foodParams = n;
		}

		{
			DiseaseParams[] n = Arrays.copyOf(diseaseParams, count);

			for (int i = 0; i < this.diseaseParams.length && i < count; i++) {
				n[i].resize();
			}

			for (int i = this.diseaseParams.length; i < count; i++) {
				n[i] = new DiseaseParams(envParams);
			}

			this.diseaseParams = n;
		}
		{
			ProductionParams[] n = Arrays.copyOf(prodParams, count);

			for (int i = prodParams.length; i < count; i++) {
				n[i] = new ProductionParams();
				n[i].type = i;
			}
			this.prodParams = n;
		}
		{
			this.geneticParams.resize(envParams);
		}
		{
			this.tempParams.resize(envParams);
		}
		{
			this.learningParams.resize(envParams);
		}
		{
			this.controllerParams.resize(envParams);
		}

	}

	public ControllerParams getControllerParams() {
		return controllerParams;
	}

	public void setControllerParams(ControllerParams params) {
		controllerParams = params;
	}

	public boolean isContinuation() {
		return
				envParams.keepOldAgents ||
				envParams.keepOldArray ||
				envParams.keepOldPackets ||
				envParams.keepOldWaste;
	}

	@Override
	public List<String> getPluginParameters() {
		List<String> result = new ArrayList<String>();
		if (this.prodParams != null && this.prodParams[0] != null)
			result.addAll(this.prodParams[0].getStatePluginKeys());
		result.addAll(this.tempParams.getStatePluginKeys());

		return result;
	}

	@Override
	public AgentFoodCountable getCounts() {
		return this.envParams;
	}

}
