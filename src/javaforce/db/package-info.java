/**
 * package javaforce.db;
 *
 * These classes provide a framework for persistent app data storage.
 *
 * Classes that derive from Row are records that are serialized and saved to disk.
 * Table stores a collection of Rows.
 * TableList stores a collection of Tables (each Table is identified by field 'id').
 * TableLog is a transactional Table for event based Rows stored in a folder.
 *
 */

package javaforce.db;
