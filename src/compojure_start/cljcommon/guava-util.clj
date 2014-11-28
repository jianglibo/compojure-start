(ns compojure-start.cljcommon.clj-util
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.nio.file Paths)
           (java.io PushbackReader)
           (com.google.common.base Charsets)
           (com.google.common.io CharStreams)
           (java.util UUID Set HashMap)))

(def US-ASCII Charsets/US_ASCII)
(def ISO-8859-1 Charsets/ISO_8859_1)
(def UTF-8 Charsets/UTF_8)
(def UTF-16BE Charsets/UTF_16BE)
(def UTF-16LE Charsets/UTF_16LE)
(def UTF-16 Charsets/UTF_16)




;(io/reader "abc" :encoding "UTF-8")


(comment

ByteStreams and CharStreams
byte[] toByteArray(InputStream)	String toString(Readable)
N/A	List<String> readLines(Readable)
long copy(InputStream, OutputStream)	long copy(Readable, Appendable)
void readFully(InputStream, byte[])	N/A
void skipFully(InputStream, long)	void skipFully(Reader, long)
OutputStream nullOutputStream()	Writer nullWriter()

Reading	ByteSource	CharSource
Writing	ByteSink	CharSink


Files.asByteSource(File)	Files.asCharSource(File, Charset)
Files.asByteSink(File, FileWriteMode...)	Files.asCharSink(File, Charset, FileWriteMode...)
Resources.asByteSource(URL)	Resources.asCharSource(URL, Charset)
ByteSource.wrap(byte[])	CharSource.wrap(CharSequence)
ByteSource.concat(ByteSource...)	CharSource.concat(CharSource...)
ByteSource.slice(long, long)	N/A
N/A	ByteSource.asCharSource(Charset)
N/A	ByteSink.asCharSink(Charset)

ByteSource	CharSource
byte[] read()	String read()
N/A	ImmutableList<String> readLines()
N/A	String readFirstLine()
long copyTo(ByteSink)	long copyTo(CharSink)
long copyTo(OutputStream)	long copyTo(Appendable)
long size() (in bytes)	N/A
boolean isEmpty()	boolean isEmpty()
boolean contentEquals(ByteSource)	N/A
HashCode hash(HashFunction)	N/A

ByteSink	CharSink
void write(byte[])	void write(CharSequence)
long writeFrom(InputStream)	long writeFrom(Readable)
N/A	void writeLines(Iterable<? extends CharSequence>)
N/A	void writeLines(Iterable<? extends CharSequence>, String)


createParentDirs(File)	Creates necessary but nonexistent parent directories of the file.
getFileExtension(String)	Gets the file extension of the file described by the path.
getNameWithoutExtension(String)	Gets the name of the file with its extension removed
simplifyPath(String)	Cleans up the path. Not always consistent with your filesystem; test carefully!
fileTreeTraverser()	Returns a TreeTraverser that can traverse file trees)
