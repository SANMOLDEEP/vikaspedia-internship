package backend.service;

import backend.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OperationCompositionService {
    
    /**
     * Compose multiple operations into a single optimized operation
     */
    public List<Operation> composeOperations(List<Operation> operations) {
        if (operations.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Operation> composed = new ArrayList<>();
        Operation current = null;
        
        for (Operation op : operations) {
            if (current == null) {
                current = op;
            } else if (canCompose(current, op)) {
                current = composeTwo(current, op);
            } else {
                composed.add(current);
                current = op;
            }
        }
        
        if (current != null) {
            composed.add(current);
        }
        
        return composed;
    }
    
    /**
     * Check if two operations can be composed
     */
    private boolean canCompose(Operation op1, Operation op2) {
        // Only compose operations from same user and document
        if (!op1.getDocumentId().equals(op2.getDocumentId()) || 
            !op1.getUserId().equals(op2.getUserId())) {
            return false;
        }
        
        // Compose consecutive operations
        if (op1 instanceof InsertOperation && op2 instanceof InsertOperation) {
            InsertOperation insert1 = (InsertOperation) op1;
            InsertOperation insert2 = (InsertOperation) op2;
            return insert1.getPosition() + insert1.getContent().length() == insert2.getPosition();
        }
        
        if (op1 instanceof DeleteOperation && op2 instanceof DeleteOperation) {
            DeleteOperation delete1 = (DeleteOperation) op1;
            DeleteOperation delete2 = (DeleteOperation) op2;
            return delete1.getPosition() == delete2.getPosition();
        }
        
        return false;
    }
    
    /**
     * Compose two operations into one
     */
    private Operation composeTwo(Operation op1, Operation op2) {
        if (op1 instanceof InsertOperation && op2 instanceof InsertOperation) {
            InsertOperation insert1 = (InsertOperation) op1;
            InsertOperation insert2 = (InsertOperation) op2;
            
            return new InsertOperation(
                insert1.getDocumentId(),
                insert1.getUserId(),
                Math.min(insert1.getTimestamp(), op2.getTimestamp()),
                insert1.getSequenceNumber(),
                insert1.getPosition(),
                insert1.getContent() + insert2.getContent()
            );
        }
        
        if (op1 instanceof DeleteOperation && op2 instanceof DeleteOperation) {
            DeleteOperation delete1 = (DeleteOperation) op1;
            DeleteOperation delete2 = (DeleteOperation) op2;
            
            return new DeleteOperation(
                delete1.getDocumentId(),
                delete1.getUserId(),
                Math.min(delete1.getTimestamp(), op2.getTimestamp()),
                delete1.getSequenceNumber(),
                delete1.getPosition(),
                delete1.getLength() + delete2.getLength()
            );
        }
        
        return op1;
    }
    
    /**
     * Optimize operations by removing redundant ones
     */
    public List<Operation> optimizeOperations(List<Operation> operations) {
        List<Operation> optimized = new ArrayList<>();
        
        for (Operation op : operations) {
            if (!isRedundant(op, optimized)) {
                optimized.add(op);
            }
        }
        
        return optimized;
    }
    
    /**
     * Check if an operation is redundant given previous operations
     */
    private boolean isRedundant(Operation op, List<Operation> previousOps) {
        for (Operation prev : previousOps) {
            if (cancelsOut(prev, op)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if two operations cancel each other out
     */
    private boolean cancelsOut(Operation op1, Operation op2) {
        if (op1 instanceof InsertOperation && op2 instanceof DeleteOperation) {
            InsertOperation insert = (InsertOperation) op1;
            DeleteOperation delete = (DeleteOperation) op2;
            return insert.getPosition() == delete.getPosition() && 
                   insert.getContent().length() == delete.getLength();
        }
        
        if (op1 instanceof DeleteOperation && op2 instanceof InsertOperation) {
            DeleteOperation delete = (DeleteOperation) op1;
            InsertOperation insert = (InsertOperation) op2;
            return delete.getPosition() == insert.getPosition() && 
                   delete.getLength() == insert.getContent().length();
        }
        
        return false;
    }
    
    /**
     * Create a compact operation representation for large documents
     */
    public CompactOperation createCompactOperation(List<Operation> operations) {
        if (operations.isEmpty()) {
            return null;
        }
        
        CompactOperation compact = new CompactOperation();
        compact.setDocumentId(operations.get(0).getDocumentId());
        compact.setUserId(operations.get(0).getUserId());
        compact.setTimestamp(System.currentTimeMillis());
        
        List<CompactOperation.Change> changes = new ArrayList<>();
        
        for (Operation op : operations) {
            if (op instanceof InsertOperation) {
                InsertOperation insert = (InsertOperation) op;
                changes.add(new CompactOperation.Change(
                    CompactOperation.ChangeType.INSERT,
                    insert.getPosition(),
                    insert.getContent()
                ));
            } else if (op instanceof DeleteOperation) {
                DeleteOperation delete = (DeleteOperation) op;
                changes.add(new CompactOperation.Change(
                    CompactOperation.ChangeType.DELETE,
                    delete.getPosition(),
                    String.valueOf(delete.getLength())
                ));
            }
        }
        
        compact.setChanges(changes);
        return compact;
    }
    
    /**
     * Expand compact operation back to list of operations
     */
    public List<Operation> expandCompactOperation(CompactOperation compact) {
        List<Operation> operations = new ArrayList<>();
        
        for (CompactOperation.Change change : compact.getChanges()) {
            if (change.getType() == CompactOperation.ChangeType.INSERT) {
                operations.add(new InsertOperation(
                    compact.getDocumentId(),
                    compact.getUserId(),
                    compact.getTimestamp(),
                    0, // sequence number will be set by OT service
                    change.getPosition(),
                    change.getContent()
                ));
            } else if (change.getType() == CompactOperation.ChangeType.DELETE) {
                operations.add(new DeleteOperation(
                    compact.getDocumentId(),
                    compact.getUserId(),
                    compact.getTimestamp(),
                    0, // sequence number will be set by OT service
                    change.getPosition(),
                    Integer.parseInt(change.getContent())
                ));
            }
        }
        
        return operations;
    }
}
