import java.util.LinkedList;
import java.util.Queue;

public class ClusterMembership {
    //Most code here was adapted from https://www.javatpoint.com/binary-tree-java and https://www.geeksforgeeks.org/binary-search-tree-set-2-delete/

    //Represent a node of binary tree    
    public static class Node  
    {    
        Member data;    
        Node left;    
        Node right;    
        public Node(Member data)  
            {    
              //Assign data to the new node, set left and right children to null    
              this.data = data;    
              this.left = null;    
              this.right = null;    
            }    
    }
    
    public Node root;   

    public ClusterMembership(){    
        root = null;    
    }  
    
    public void insert(Member value)   
    {  
        if(root==null)
            root = new Node(value);
        else
            insertRec(root, value);
    } 
    
    private void insertRec(Node node, Member value) {

        int compare = value.hashKey.compareTo(node.data.hashKey);
        if (compare<0)   
        {  
            if (node.left != null)   
            {  
                insertRec(node.left, value);  
            } else   
            {  
                node.left = new Node(value);  
            }  
        }   
        else if (compare>0)   
        {  
            if (node.right != null)   
            {  
                insertRec(node.right, value);  
            } else   
            {  
                node.right = new Node(value);  
            }  
        }  
    }

    public void delete(Member key)  
{  
    if (root == null)
        return;
    else
        deleteRec(root,key);
}
   
    private Node deleteRec(Node root, Member key) {
    // Base case
    if (root == null)
         return root;
    int compare = root.data.hashKey.compareTo(key.hashKey);
    // Recursive calls for ancestors of
    // node to be deleted
    if (compare>0)
     {
        root.left = deleteRec(root.left, key);
        return root;
    }
    else if (compare<0)
    {
        root.right = deleteRec(root.right, key);
        return root;
    }
 
    // We reach here when root is the node
    // to be deleted.
 
    // If one of the children is empty
    if (root.left == null)
    {
        Node temp = root.right;
        return temp;
    }
    else if (root.right == null)
    {
        Node temp = root.left;
        return temp;
    }
 
    // If both children exist
    else
    {
        Node succParent = root;
 
        // Find successor
        Node succ = root.right;
         
        while (succ.left != null)
        {
            succParent = succ;
            succ = succ.left;
        }
 
        // Delete successor. Since successor
        // is always left child of its parent
        // we can safely make successor's right
        // right child as left of its parent.
        // If there is no succ, then assign
        // succ->right to succParent->right
        if (succParent != root)
            succParent.left = succ.right;
        else
            succParent.right = succ.right;
 
        // Copy Successor Data to root
        root.data = succ.data;
 
        return root;
    }
    }

    public void show()
    {
        System.out.println("SHOW: \n");
        inorder(root);
    }
    private void inorder(Node root)
    {
        if (root != null)
        {
            inorder(root.left);
            System.out.print(root.data.hashKey + " - " + root.data.ipAddress+ " - " + root.data.counter + "\n");
            inorder(root.right);
        }
    }
}
