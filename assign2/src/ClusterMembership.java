import java.util.ArrayList;
import java.util.List;

public class ClusterMembership {
    //Code here was adapted from https://www.javatpoint.com/binary-tree-java

    public static class Node  
    {    
        Member data;    
        Node left;    
        Node right;    
        public Node(Member data)  
            {    
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
        else
        {
            if(value.counter>node.data.counter)
                node.data.counter=value.counter;
        }  
    }

    public void setCounter(Member searchedValue, int counter)  
  {  
    Node current = root;  
    while(!current.data.ipAddress.equals(searchedValue.ipAddress))  
    {  
        int compare = searchedValue.hashKey.compareTo(current.data.hashKey);
      if(compare < 0)  
        current = current.left;  
      else  
        current = current.right;  
      if(current == null)  
      {  
        return;  
      }  
    }  
    current.data.counter=counter;  
  }  

    public String show()
    {
       return inorder(root,"\n");
    }
    private String inorder(Node root, String string)
    {
        if (root != null)
        {
            string = inorder(root.left,string);
            string = string + root.data.hashKey + " - " + root.data.ipAddress+ " - " + root.data.counter + "\n";
            string = inorder(root.right,string);
            
            return string;
        }
        else
            return string;
    }

    public Member findSucessor(String value){
        Node node = inOrderSearchSucessor(root, value);
        if(node==null)
            node=findMinimumPresent(root);
        return node.data;
    }

    public Member findPredecessor(String value)
    { 
        List<Node> list = new ArrayList<Node>();
        inOrder(root, list);
        Node first = list.get(0);
        for(int i = 0; i<list.size()-1;i++)
        { 
            if(list.get(i+1).data.hashKey.equals(value))
                return list.get(i).data;
        }
        if(first.data.hashKey.equals(value))
            return list.get(list.size()-1).data;
        return first.data;
    }

    private Node findMinimumPresent(Node node) {
        List<Node> list = new ArrayList<Node>();
        inOrder(node, list);
        return list.get(0);
    }
	
	public void inOrder(Node node, List<Node> list){
		if(node != null){
			inOrder(node.left, list);
            if(node.data.isInsideCluster())
			    list.add(node);
			inOrder(node.right, list);	
		}
	}

    private Node inOrderSearchSucessor(Node node, String value){
        if(node == null)
            return null;
        Node left = inOrderSearchSucessor(node.left,value);
        if(left!=null){
                return left;
        }

        if(node.data.hashKey.compareTo(value)>0 && node.data.isInsideCluster())
            return node;
        else
            return inOrderSearchSucessor(node.right, value);
    }   

}

