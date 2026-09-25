/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.importdata;

import java.util.HashMap;
import java.util.Map;

/**
 * MCPServerIntegration provides utilities to integrate the MCP server with MyTourBook.
 * This class demonstrates how to create resources and tools that can be exposed to AI models.
 */
public class MCPServerIntegration {

	/**
	 * Tool definition for MCP server
	 */
	public static class MCPTool {

		public String	name;
		public String	description;
		public String	inputSchema;

		public MCPTool(final String name, final String description, final String inputSchema) {
			this.name = name;
			this.description = description;
			this.inputSchema = inputSchema;
		}
	}

	/**
	 * Resource definition for MCP server
	 */
	public static class MCPResource {

		public String	uri;
		public String	mimeType;
		public String	description;

		public MCPResource(final String uri, final String mimeType, final String description) {
			this.uri = uri;
			this.mimeType = mimeType;
			this.description = description;
		}
	}

	private static final Map<String, MCPTool>		_allTools		= new HashMap<>();
	private static final Map<String, MCPResource>	_allResources	= new HashMap<>();

	static {
		// Initialize default tools
		initializeDefaultTools();
		initializeDefaultResources();
	}

	/**
	 * Initialize default tools that can be used by the MCP server
	 */
	public static void initializeDefaultTools() {

		// Tool to query tours
		_allTools.put("query_tours", new MCPTool(
				"query_tours",
				"Query tours from the MyTourBook database",
				"{\"type\":\"object\",\"properties\":{\"filters\":{\"type\":\"object\"}}}"));

		// Tool to get tour statistics
		_allTools.put("get_tour_statistics", new MCPTool(
				"get_tour_statistics",
				"Get statistics about tours (average speed, distance, etc.)",
				"{\"type\":\"object\",\"properties\":{\"tour_id\":{\"type\":\"string\"}}}"));

		// Tool to import tour data
		_allTools.put("import_tour_data", new MCPTool(
				"import_tour_data",
				"Import tour data from files",
				"{\"type\":\"object\",\"properties\":{\"file_path\":{\"type\":\"string\"}}}"));

		// Tool to export tour data
		_allTools.put("export_tour_data", new MCPTool(
				"export_tour_data",
				"Export tour data to various formats",
				"{\"type\":\"object\",\"properties\":{\"tour_id\":{\"type\":\"string\"},\"format\":{\"type\":\"string\"}}}"));
	}

	/**
	 * Initialize default resources that can be accessed by the MCP server
	 */
	public static void initializeDefaultResources() {

		// Tours resource
		_allResources.put("tours", new MCPResource(
				"mytourbook://tours",
				"application/json",
				"All tour data in the MyTourBook database"));

		// Statistics resource
		_allResources.put("statistics", new MCPResource(
				"mytourbook://statistics",
				"application/json",
				"Tour statistics and summaries"));

		// Settings resource
		_allResources.put("settings", new MCPResource(
				"mytourbook://settings",
				"application/json",
				"MyTourBook application settings"));
	}

	/**
	 * Register a custom tool with the MCP server
	 *
	 * @param tool the tool to register
	 * @return true if registration was successful
	 */
	public static synchronized boolean registerTool(final MCPTool tool) {

		if (tool == null || tool.name == null) {
			return false;
		}

		_allTools.put(tool.name, tool);

		final MCPServerManager manager = MCPServerManager.getInstance();
		if (manager.isServerRunning()) {
			return manager.registerResourceTool(tool.name, tool.description);
		}

		return true;
	}

	/**
	 * Register a custom resource with the MCP server
	 *
	 * @param resource the resource to register
	 * @return true if registration was successful
	 */
	public static synchronized boolean registerResource(final MCPResource resource) {

		if (resource == null || resource.uri == null) {
			return false;
		}

		_allResources.put(resource.uri, resource);

		return true;
	}

	/**
	 * Get a tool by name
	 *
	 * @param toolName the name of the tool
	 * @return the MCPTool or null if not found
	 */
	public static MCPTool getTool(final String toolName) {
		return _allTools.get(toolName);
	}

	/**
	 * Get a resource by URI
	 *
	 * @param resourceUri the URI of the resource
	 * @return the MCPResource or null if not found
	 */
	public static MCPResource getResource(final String resourceUri) {
		return _allResources.get(resourceUri);
	}

	/**
	 * Get all registered tools
	 *
	 * @return map of all tools
	 */
	public static Map<String, MCPTool> getAllTools() {
		return new HashMap<>(_allTools);
	}

	/**
	 * Get all registered resources
	 *
	 * @return map of all resources
	 */
	public static Map<String, MCPResource> getAllResources() {
		return new HashMap<>(_allResources);
	}

	/**
	 * Example method showing how to use the MCP server
	 */
	public static void demonstrateUsage() {

		System.out.println("[MCPServerIntegration] Demonstrating MCP Server usage..."); //$NON-NLS-1$

		final MCPServerManager manager = MCPServerManager.getInstance();

		// Initialize the server
		if (!manager.initializeServer()) {
			System.err.println("[MCPServerIntegration] Failed to initialize server"); //$NON-NLS-1$
			return;
		}

		// Register all default tools
		for (final MCPTool tool : _allTools.values()) {
			manager.registerResourceTool(tool.name, tool.description);
		}

		// Start the server
		if (!manager.startServer()) {
			System.err.println("[MCPServerIntegration] Failed to start server"); //$NON-NLS-1$
			return;
		}

		System.out.println("[MCPServerIntegration] MCP Server is running with " + _allTools.size() + " tools and " //$NON-NLS-1$ //$NON-NLS-2$
				+ _allResources.size() + " resources"); //$NON-NLS-1$
	}
}
