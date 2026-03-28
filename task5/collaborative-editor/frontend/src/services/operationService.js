// Operational Transformation service for frontend

// Generate operations for text changes
export const textChangeToOperations = (oldText, newText, userId) => {
  const operations = [];
  const timestamp = Date.now();
  
  // Find the common prefix
  let commonPrefix = 0;
  const minLength = Math.min(oldText.length, newText.length);
  while (commonPrefix < minLength && oldText.charAt(commonPrefix) === newText.charAt(commonPrefix)) {
    commonPrefix++;
  }
  
  // Find the common suffix
  let commonSuffix = 0;
  while (commonSuffix < minLength - commonPrefix && 
         oldText.charAt(oldText.length - 1 - commonSuffix) === newText.charAt(newText.length - 1 - commonSuffix)) {
    commonSuffix++;
  }
  
  // Calculate the middle parts
  const oldMiddle = oldText.substring(commonPrefix, oldText.length - commonSuffix);
  const newMiddle = newText.substring(commonPrefix, newText.length - commonSuffix);
  
  // Delete old middle
  if (oldMiddle.length > 0) {
    operations.push({
      type: "delete",
      documentId: "doc1",
      userId: userId,
      timestamp: timestamp,
      sequenceNumber: 0, // Will be set by server
      position: commonPrefix,
      length: oldMiddle.length
    });
  }
  
  // Insert new middle
  if (newMiddle.length > 0) {
    operations.push({
      type: "insert",
      documentId: "doc1",
      userId: userId,
      timestamp: timestamp,
      sequenceNumber: 0, // Will be set by server
      position: commonPrefix,
      content: newMiddle
    });
  }
  
  return operations;
};

// Apply operations to text
export const applyOperations = (text, operations) => {
  let result = text;
  
  for (const operation of operations) {
    if (operation.type === "insert") {
      const position = Math.min(operation.position, result.length);
      result = result.substring(0, position) + operation.content + result.substring(position);
    } else if (operation.type === "delete") {
      const position = Math.min(operation.position, result.length);
      const length = Math.min(operation.length, result.length - position);
      result = result.substring(0, position) + result.substring(position + length);
    }
    // Retain operations don't modify content
  }
  
  return result;
};

// Transform two operations against each other
export const transformOperations = (op1, op2) => {
  // Simple transformation logic
  if (op1.type === "insert" && op2.type === "insert") {
    if (op2.position <= op1.position) {
      return {
        ...op1,
        position: op1.position + op2.content.length
      };
    }
  } else if (op1.type === "insert" && op2.type === "delete") {
    if (op2.position < op1.position) {
      return {
        ...op1,
        position: Math.max(op2.position, op1.position - op2.length)
      };
    }
  } else if (op1.type === "delete" && op2.type === "insert") {
    if (op2.position <= op1.position) {
      return {
        ...op1,
        position: op1.position + op2.content.length
      };
    }
  } else if (op1.type === "delete" && op2.type === "delete") {
    if (op2.position + op2.length <= op1.position) {
      return {
        ...op1,
        position: op1.position - op2.length
      };
    } else if (op2.position < op1.position + op1.length) {
      // Overlapping deletes - adjust length
      const overlap = Math.min(op1.position + op1.length, op2.position + op2.length) - 
                      Math.max(op1.position, op2.position);
      return {
        ...op1,
        length: op1.length - overlap
      };
    }
  }
  
  return op1;
};

// Compose multiple operations into compact format
export const composeOperations = (operations) => {
  if (operations.length === 0) return null;
  
  const compact = {
    type: "compact",
    documentId: operations[0].documentId,
    userId: operations[0].userId,
    timestamp: Date.now(),
    changes: []
  };
  
  for (const op of operations) {
    if (op.type === "insert") {
      compact.changes.push({
        type: "INSERT",
        position: op.position,
        content: op.content
      });
    } else if (op.type === "delete") {
      compact.changes.push({
        type: "DELETE", 
        position: op.position,
        content: String(op.length)
      });
    }
  }
  
  return compact;
};

// Apply compact operation to text
export const applyCompactOperation = (text, compact) => {
  let result = text;
  
  // Apply changes in reverse order to maintain position accuracy
  for (let i = compact.changes.length - 1; i >= 0; i--) {
    const change = compact.changes[i];
    
    if (change.type === "INSERT") {
      const position = Math.min(change.position, result.length);
      result = result.substring(0, position) + change.content + result.substring(position);
    } else if (change.type === "DELETE") {
      const position = Math.min(change.position, result.length);
      const length = Math.min(parseInt(change.content), result.length - position);
      result = result.substring(0, position) + result.substring(position + length);
    }
  }
  
  return result;
};

// Optimize operations by composing consecutive ones
export const optimizeOperations = (operations) => {
  if (operations.length === 0) return [];
  
  const optimized = [];
  let current = operations[0];
  
  for (let i = 1; i < operations.length; i++) {
    const next = operations[i];
    
    if (canCompose(current, next)) {
      current = composeTwo(current, next);
    } else {
      optimized.push(current);
      current = next;
    }
  }
  
  optimized.push(current);
  return optimized;
};

// Check if two operations can be composed
const canCompose = (op1, op2) => {
  if (op1.type === "insert" && op2.type === "insert") {
    return op1.position + op1.content.length === op2.position;
  }
  
  if (op1.type === "delete" && op2.type === "delete") {
    return op1.position === op2.position;
  }
  
  return false;
};

// Compose two operations
const composeTwo = (op1, op2) => {
  if (op1.type === "insert" && op2.type === "insert") {
    return {
      ...op1,
      content: op1.content + op2.content
    };
  }
  
  if (op1.type === "delete" && op2.type === "delete") {
    return {
      ...op1,
      length: op1.length + op2.length
    };
  }
  
  return op1;
};
