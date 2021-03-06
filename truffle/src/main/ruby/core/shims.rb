# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# These are implemented just to get other stuff working - we'll go back and
# implement these properly later.

# Here otherwise it causes problems for RubySpec
class Channel
end

class IO
  def external_encoding
    @external
  end

  def internal_encoding
    @internal
  end

  def set_encoding(external, internal)
    @external = external
    @internal = internal
  end
end

STDIN = IO.new

class << STDIN
  def external_encoding
    super || Encoding.default_external
  end
end

STDOUT = IO.new
$stdout = STDOUT

class << STDOUT
  def puts(*values)
    Kernel.send(:puts, *values)
  end

  def print(*values)
    Kernel.send(:print, *values)
  end

  def printf(*values)
    Kernel.send(:printf, *values)
  end

  def flush
    Truffle::Debug.flush_stdout
  end

  def sync
    false
  end

  def sync=(value)
  end
end

STDERR = IO.new

class << STDERR
  def self.puts(*values)
    Kernel.send(:puts, *values)
  end
end

ARGF = Object.new

class Hash

  def fetch(key, default=nil)
    if key?(key)
      self[key]
    elsif block_given?
      yield(key)
    elsif default
      default
    else
      raise(KeyError, "key not found: #{key}")
    end
  end

  def each_key
    each do |key, value|
      yield key
    end
  end

  def each_value
    each do |key, value|
      yield value
    end
  end

  def value?(value)
    values.any? { |v| v == value }
  end

  alias_method :has_value?, :value?

end

class Regexp
  def self.last_match(n = nil)
    if n
      # TODO (nirvdrum Jan. 8, 2015) Make sure this supports symbol keys for named capture lookup.
      $~.values_at(n).first
    else
      $~
    end
  end
end

module Rubinius

  L64 = true

  def extended_modules(object)
    []
  end

end

class Module

  def extended_modules(object)
    []
  end

end

class String
  def append(other)
    self << other
  end
end

class Array
  def rindex(obj)
    index = nil

    each_with_index do |e, i|
      index = i if e == obj
    end

    index
  end
end

module Kernel
  def inspect
    ivars = instance_variables

    return to_s if ivars.empty?

    prefix = "#<#{self.class}:0x#{self.object_id.to_s(16)}"

    parts = []
    ivars.each do |var|
      parts << "#{var}=#{instance_variable_get(var).inspect}"
    end

    "#{prefix} #{parts.join(', ')}>"
  end
end

class Rational

  alias :__slash__ :/

  def _offset_to_milliseconds
    (self * 1000).to_i
  end

end

class BasicObject

  def __id__
    Rubinius.primitive :object_id
  end

end

ENV['TZ'] = 'UTC'

class MatchData
  def full
    @cached_full ||= begin
      tuple = Rubinius::Tuple.new
      tuple << self.begin(0)
      tuple << self.end(0)
      tuple
    end
  end
end

# Wrapper class for Rubinius's exposure of @data within String.
#
# We can't use Array directly because we don't currently guarantee that we'll always return the same
# exact underlying byte array.  Rubinius calls #equal? rather than #== throughout its code, making a tighter
# assumption than we provide.  This wrapper provides the semantics we need in the interim.
module Rubinius
  class StringData
    attr_accessor :array

    def initialize(array)
      @array = array
    end

    def equal?(other)
      @array == other.array
    end

    alias_method :==, :equal?

    def size
      @array.size
    end

    def [](index)
      @array[index]
    end
  end
end