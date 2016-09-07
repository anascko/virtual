#!/bin/bash
# Check MOS or devsteck
export LC_ALL="en_US.UTF-8"
VNET=`(brctl show | awk '/p4p2/ {print $1}')`
VNET1=`(echo $VNET | cut -c1-4)`
#declare -a name_vm
name_vm=""
if [[ $VNET1 == "fuel" ]]
    then
      for vm in $(virsh list --name)
        do 
          name_vm=`virsh dumpxml $vm|grep -q "$VNET" && echo $vm`
        done
#VNET1=`(ifconfig $VNET | awk '/HWaddr/ {print $5}')`
     else 
#it's devsteck
     adr_node=`(ifconfig $VNET | awk '/addr:/ {print  $2}' |cut -c6-21 | cut -d. -f 1,2,3)`
#     echo $adr_node
     mac=`(sudo arping -c 1 $adr_node.1 | awk '/bytes/ {print $1}')`
#echo $mac
    if [[ $mac == '42' ]]
      then
        VNET1=`(sshpass -p r00tme ssh root@$adr_node.1 'ifconfig eth1 | grep HWaddr' | awk '{print $5}')`
        for vm in $(virsh list --name)
           do
            mac_vm=""
            mac_vm=`virsh dumpxml $vm|grep "$VNET1"`
              if [[ $mac_vm != "" ]]
                 then 
                    name_vm=$vm
	      fi 
           done
#        echo $name_vm
      else
          exit 1
    fi
fi
# adr_node=`(ifconfig $VNET | awk '/addr:/ {print  $2}' |cut -c6-21)
echo "Please ask's admin of $name_vm turn off bridge $VNET from p4p2"
