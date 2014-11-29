package us.moura.report;

import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A very simple XML to PDF report parser.
 * 
 * @author sergio
 *
 */
public class Report {
	protected Object model;
	protected URL reportUrl;
	
	private List<Node> nodes;
	
	private TableTextNodeHandler handler = null;
	private TableTextNodeHandler currentTableHandler = null;

	public Report() {
		model = null;
		reportUrl = null;
		nodes = null;
	}

	public Report(URL url, Object model) {
		reportUrl = url;
		this.model = model;
		nodes = null;
	}
	
	public void setModel(Object model) {
		this.model = model;
	}
	
	public void setURL(URL url) {
		this.reportUrl = url;
	}
	
	public void savePDF(String filename, List<Object> models) {
		Document xmlDocument;
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			xmlDocument = builder.parse(reportUrl.openStream());
		} catch (ParserConfigurationException | SAXException | IOException exception) {
			// TODO Auto-generated catch block
			exception.printStackTrace();
			return;
		}
		
		PDDocument document = new PDDocument();

		for (Object model : models) {
			// Create a new blank page and add it to the document
			PDPage page = new PDPage(PDPage.PAGE_SIZE_A4);
			document.addPage(page);		
	
			try {
				populatePageFromXML(document, page, model, xmlDocument);
			} catch (ParserConfigurationException | SAXException | IOException e1) {
				e1.printStackTrace();
			}
		}

		try {
			document.save(filename);
			document.close();
		} catch (COSVisitorException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void savePDF(String filename) {		
		Document xmlDocument;
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			xmlDocument = builder.parse(reportUrl.openStream());
		} catch (ParserConfigurationException | SAXException | IOException exception) {
			// TODO Auto-generated catch block
			exception.printStackTrace();
			return;
		}
		
		PDDocument document = new PDDocument();

		// Create a new blank page and add it to the document
		PDPage page = new PDPage(PDPage.PAGE_SIZE_A4);
		document.addPage(page);		

		try {
			populatePageFromXML(document, page, model, xmlDocument);
		} catch (ParserConfigurationException | SAXException | IOException e1) {
			e1.printStackTrace();
		}

		try {
			document.save(filename);
			document.close();
		} catch (COSVisitorException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private PDFont getFontFromName(String fontName) {
		PDFont font = PDType1Font.HELVETICA;
		if ("helvetica".compareToIgnoreCase(fontName) == 0) {
			font = PDType1Font.HELVETICA;
		} else if ("helvetica bold".compareToIgnoreCase(fontName) == 0) {
			font = PDType1Font.HELVETICA_BOLD;
		} else if ("helvetica oblique".compareToIgnoreCase(fontName) == 0
				|| "helvetica italic".compareToIgnoreCase(fontName) == 0) {
			font = PDType1Font.HELVETICA_OBLIQUE;
		} else if ("helvetica bold oblique".compareToIgnoreCase(fontName) == 0
				|| "helvetica bold italic".compareToIgnoreCase(fontName) == 0
				|| "helvetica oblique bold".compareToIgnoreCase(fontName) == 0
				|| "helvetica italic bold".compareToIgnoreCase(fontName) == 0) {
			font = PDType1Font.HELVETICA_BOLD_OBLIQUE;
		} else if ("times".compareToIgnoreCase(fontName) == 0) {
			font = PDType1Font.TIMES_ROMAN;
		} else if ("times bold".compareToIgnoreCase(fontName) == 0) {
			font = PDType1Font.TIMES_BOLD;
		} else if ("times oblique".compareToIgnoreCase(fontName) == 0
				|| "times italic".compareToIgnoreCase(fontName) == 0) {
			font = PDType1Font.TIMES_ITALIC;
		} else if ("times bold oblique".compareToIgnoreCase(fontName) == 0
				|| "times bold italic".compareToIgnoreCase(fontName) == 0
				|| "times oblique bold".compareToIgnoreCase(fontName) == 0
				|| "times italic bold".compareToIgnoreCase(fontName) == 0) {
			font = PDType1Font.TIMES_BOLD_ITALIC;
		}

		return (font);
	}

	/** Text nodes */
	private void handleTextNode(PDPageContentStream contentStream, Node node, Object model, float xOffset, float yOffset) throws IOException {
		String text = "";
		float xPos = xOffset;
		float yPos = yOffset;
		PDFont font = PDType1Font.HELVETICA;
		float fontSize = 12;

		Format formatter = null;

		Node aux = node.getAttributes().getNamedItem("x");
		if (aux != null) {
			xPos += Float.parseFloat(aux.getNodeValue());
		}

		aux = node.getAttributes().getNamedItem("y");
		if (aux != null) {
			yPos += Float.parseFloat(aux.getNodeValue());
		}

		// Parse each of the sub-values
		for (int j = 0; j < node.getChildNodes().getLength(); j++) {
			Node childNode = node.getChildNodes().item(j);

			// Font
			if (childNode.getNodeName().compareToIgnoreCase("font") == 0) {
				// Font name
				Node attrNode = childNode.getAttributes().getNamedItem("name");
				if (attrNode != null) {
					String fontName = attrNode.getNodeValue();
					font = getFontFromName(fontName);
				}
				// Font size
				attrNode = childNode.getAttributes().getNamedItem("size");
				if (attrNode != null) {
					fontSize = Float.parseFloat(attrNode.getNodeValue());
				}
			}
			// Text
			else if (childNode.getNodeName().compareToIgnoreCase("string") == 0) {
				text = childNode.getTextContent();
			}
			// Format
			else if (childNode.getNodeName().compareToIgnoreCase("format") == 0) {
				// TODO: Formatter!
				// String pattern =
				// childNode.getAttributes().getNamedItem("pattern").getNodeValue();
				String pattern = "#,##0.00";
				formatter = new DecimalFormat(pattern);
			}
		}

		// Draw the text
		contentStream.beginText();
		contentStream.moveTextPositionByAmount(xPos, yPos);
		contentStream.setFont(font, fontSize);
		String newText = processText(text, model, formatter);
		contentStream.drawString(newText);
		contentStream.moveTextPositionByAmount(-xPos, -yPos);
		contentStream.endText();
	}

	/** Table nodes */
	private void handleTableNode(PDPageContentStream contentStream, Node node, Object model, float xOffset, float yOffset) throws IOException {
		String listKey = node.getAttributes().getNamedItem("list-key")
				.getNodeValue();
		float lineHeight = 10;
		Node aux = node.getAttributes().getNamedItem("line-height");
		if (aux != null)
			lineHeight = Float.parseFloat(aux.getNodeValue());
		
		Node summaryNode = null;

		try {
			Object objectList = getParameterFromObject(model, listKey);
			List<?> objects = null;
			if (objectList instanceof List<?>) {
				objects = ((List<?>) objectList);
			}
			if (objects == null) {
				System.err.println("Object list of invalid type: " + objectList.getClass());
				return;
			}
			float xPos = xOffset + Float.parseFloat(node.getAttributes().getNamedItem("x").getNodeValue());
			float yPos = yOffset + Float.parseFloat(node.getAttributes().getNamedItem("y").getNodeValue());

			NodeList children = node.getChildNodes();
			for (Object o : objects) {
				for (int j = 0; j < children.getLength(); j++) {
					Node tableEntry = children.item(j);

					handleNode(contentStream, tableEntry, o, xPos, yPos);
					if ("summary".compareToIgnoreCase(tableEntry.getNodeName()) == 0)
						summaryNode = tableEntry;
				}

				yPos -= lineHeight;
			}
			
			if (summaryNode != null) {
				handleNode(contentStream, summaryNode, currentTableHandler, xPos, yPos);
			}
		} catch (SecurityException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private void handleNode(PDPageContentStream contentStream, Node node, Object model, float xOffset, float yOffset) throws IOException {
		
		this.nodes.add(node);
		if (node.getNodeName().compareToIgnoreCase("text") == 0) {
			if (handler == null)
				handleTextNode(contentStream, node, model, xOffset, yOffset);
			else
				handler.handleTextNode(contentStream, node, model, xOffset, yOffset);
		}
		else if (node.getNodeName().compareToIgnoreCase("table") == 0) {
			handleTableNode(contentStream, node, model, xOffset, yOffset);
		}
		else if (node.getNodeName().compareToIgnoreCase("summary") == 0) {
			if (currentTableHandler == null) {
				currentTableHandler = new TableTextNodeHandler();
			}
			
			if (model != currentTableHandler) {
				// If we're using our handler as model, we're printing out the stuff!
				handler = currentTableHandler;
			}
			else {
				System.out.println("Printing!");
			}
			Node parentNode = this.nodes.get(this.nodes.size() - 2);
			System.out.println("summary inside node type " + parentNode.getNodeName());
			
			NodeList children = node.getChildNodes();
			for (int j = 0; j < children.getLength(); j++) {
				Node summaryEntry = children.item(j);

				handleNode(contentStream, summaryEntry, model, xOffset, yOffset);
			}
			handler = null;
		}
		else if (node.getNodeName().compareToIgnoreCase("line") == 0) {
			float x1 = xOffset + Float.parseFloat(node.getAttributes().getNamedItem("x1").getNodeValue());
			float y1 = yOffset + Float.parseFloat(node.getAttributes().getNamedItem("y1").getNodeValue());
			float x2 = xOffset + Float.parseFloat(node.getAttributes().getNamedItem("x2").getNodeValue());
			float y2 = yOffset + Float.parseFloat(node.getAttributes().getNamedItem("y2").getNodeValue());

			contentStream.setNonStrokingColor(Color.BLACK);
			contentStream.setLineWidth(1);

			contentStream.addLine(x1, y1, x2, y2);
			contentStream.closeAndStroke();
		}
		else if (node.getNodeName().compareToIgnoreCase("#text") == 0
				|| node.getNodeName().compareToIgnoreCase("#comment") == 0) {
			// Do nothing.
		} 
		else {
			System.out.println("Unknown node type: " + node.getNodeName());
		}
		this.nodes.remove(this.nodes.size() - 1);
	}

	/**
	 * Reads the XML file defined in the url parameter and populates a given pdf
	 * document and page.
	 * 
	 * @param document
	 * @param page
	 * @param model
	 * @param xmlDocument
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	private void populatePageFromXML(PDDocument document, PDPage page, Object model, Document xmlDocument)
			throws ParserConfigurationException, SAXException, IOException {

		NodeList nodes = xmlDocument.getDocumentElement().getChildNodes();

		PDPageContentStream contentStream = new PDPageContentStream(document, page);
		
		this.nodes = new ArrayList<Node>();

		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			handleNode(contentStream, node, model, 0, 0);
		}

		contentStream.close();
	}

	/**
	 * Process a given text, looks for parameters and grab its values from the
	 * given model using the formatter.
	 * 
	 * @param text
	 * @param model
	 * @param formatter
	 * @return
	 */
	private String processText(String text, Object model, Format formatter) {
		int original = 0;
		int first = 0;
		int last = -1;

		StringBuilder sb = new StringBuilder();

		first = text.indexOf('@', 0);
		while (first > -1) {
			if (first > original) {
				sb.append(text.substring(original, first));
			}

			last = text.indexOf('@', first + 1);
			if (last > -1) {
				String expr = text.substring(first + 1, last);
				String value = processExpression(expr, model);

				if (formatter != null) {
					try {
						Object myValue = value;
						if (formatter instanceof DecimalFormat) {
							myValue = Float.parseFloat(value);
						}
						sb.append(formatter.format(myValue));
					} catch (IllegalArgumentException e) {
						System.err.println("Cannot format '" + value + "'");
						sb.append(value);
					}
				} else {
					sb.append(value);
				}
				original = last + 1;

				// Find next one.
				first = text.indexOf('@', last + 1);
			} else {
				sb.append(text.substring(first));
				first = -1;
			}
		}
		sb.append(text.substring(original));

		return (sb.toString());
	}

	private Object getParameterFromObject(Object o, String param) throws SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (o == null)
			return(null);
		
		
		Object x = null;
		try {
			String functionName = "get" + param;
			
			Method method = o.getClass().getMethod(functionName);
			x = method.invoke(o);
		}
		catch (NoSuchMethodException e) {
		}
		
		if (x == null) {
			try {
				Field paramField = o.getClass().getField(param);
				x = paramField.get(o);
			} catch (NoSuchFieldException e) {
			}
		}
		
		if (x == null) {
			if (o instanceof Map<?,?>) {
				x = ((Map<?,?>)o).get(param);
			}
		}
		
		return(x);
	}

	/**
	 * Given an expression, tries to find its value on the current model.
	 * 
	 * @param expr
	 * @param model
	 * @return String with value.
	 */
	private String processExpression(String expr, Object model) {
		Object parent = model;

		List<String> parameters = getTokenizedString(expr, '.');

		for (String param : parameters) {
			try {
				Object newParent = getParameterFromObject(parent, param);
				System.out.println("process '" + parent + "'.'" + param + "' => " + newParent);
				parent = newParent;
			} catch (SecurityException
					| IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				System.err.println("Error parsing parameter '" + expr + "'");
				return("<N/A>");
			}

		}

		System.out.println("expr " + expr + " => " + parent);
		if (parent == null)
			return(null);
		
		return (parent.toString());
	}
	
	/**
	 * Separates a string with a given token and return a List of the pieces.
	 * 
	 * @param text
	 * @param token
	 * @return
	 */
	public static List<String> getTokenizedString(String text, char token) {
		int last = 0;
		int next = text.indexOf(token, 0);

		List<String> parameters = new ArrayList<String>();
		while (next != -1) {
			parameters.add(text.substring(last, next));
			last = next + 1;
			next = text.indexOf(token, last);
		}
		parameters.add(text.substring(last));

		return(parameters);
	}
	
	/**
	 * Joins a list of strings together putting a token between the pieces.
	 * 
	 * @param list
	 * @param token
	 * @return
	 */
	public static String joinList(List<String> list, char token) {
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		for (String item : list) {
			if (first == true)
				first = false;
			else
				sb.append(token);
			
			sb.append(item);
		}
		
		return(sb.toString());
	}
	
	class TableTextNodeHandler {
		public Map<String, Float> total;
		public Map<String, Integer> count;
		
		public TableTextNodeHandler() {
			total = new HashMap<String, Float>();
			count = new HashMap<String, Integer>();
		}
		
		public Number getSummaryValue(String expr) {
			List<String> parameters = getTokenizedString(expr, '.');
			
			String type = parameters.get(0);
			parameters.remove(0);
			String key = joinList(parameters, '.');
			
			if ("total".compareToIgnoreCase(type) == 0) {
				return(total.get(key));
			}
			if ("count".compareToIgnoreCase(type) == 0) {
				return(count.get(key));
			}
			if ("average".compareToIgnoreCase(type) == 0) {
				float tot = total.get(key);
				float c = count.get(key);
				return(tot/c);
			}
			
			System.err.println("Invalid summary type: " + type);
			return(0);
		}
		
		private String processExpression(String expr, Object model) {
			Object parent = model;

			List<String> parameters = getTokenizedString(expr, '.');
			
			parameters.remove(0);
			String key = joinList(parameters, '.');
			System.out.println("Expr: " + expr + " / Key: " + key);

			for (String param : parameters) {
				try {
					parent = getParameterFromObject(parent, param);
					if (parent instanceof Number) {
						Float n;
						if (total.containsKey(key)) {
							n = total.get(key) + ((Number)parent).floatValue();
						}
						else {
							n = new Float(((Number)parent).floatValue());
						}
						
						total.put(key, n);
						
						Integer i;
						if (count.containsKey(key)) {
							i = count.get(key);
							i = i + 1;
						}
						else {
							i = new Integer(1);
						}
						count.put(key, i);
					}
				} catch (SecurityException
						| IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					System.err.println("Error parsing parameter '" + expr + "'");
					return("<N/A>");
				}
			}
			
			

			System.out.println("expr " + expr + " => " + parent);
			return (parent.toString());
		}
		
		
		private String processText(String text, Object model) {
			int original = 0;
			int first = 0;
			int last = -1;

			StringBuilder sb = new StringBuilder();

			first = text.indexOf('@', 0);
			while (first > -1) {
				if (first > original) {
					sb.append(text.substring(original, first));
				}

				last = text.indexOf('@', first + 1);
				if (last > -1) {
					String expr = text.substring(first + 1, last);
					processExpression(expr, model);

					original = last + 1;

					// Find next one.
					first = text.indexOf('@', last + 1);
				} else {
					sb.append(text.substring(first));
					first = -1;
				}
			}
			sb.append(text.substring(original));

			return (sb.toString());
		}

		public void handleTextNode(PDPageContentStream contentStream, Node node, Object model, float xOffset, float yOffset) throws IOException {
			for (int j = 0; j < node.getChildNodes().getLength(); j++) {
				Node childNode = node.getChildNodes().item(j);

				if (childNode.getNodeName().compareToIgnoreCase("string") == 0) {
					String text = childNode.getTextContent();
					processText(text, model);
				}
			}
		}
	}
}
