/**
 * Create nodes 
 *
 * Expected parameters:
 *   DEVSTACK_REPO
 *   DEVSTACK_BRANCH
 *   LOCAL_CONF
**/

def common = new com.mirantis.mk.Common()
def git = new com.mirantis.mk.Git()


def CreateVM(vm_name, kvm_soket, img){
    return sh(script:"""
    export new_img=${vm_name} &&
    export LIBVIRT_SOCKET=${kvm_soket} && 
    export old_img=${img} &&
    virt-clone ${LIBVIRT_SOCKET} -o ${old_img} -n ${new_img} --auto-clone &&
    virsh start ${new_img} &&
    mac=$(virsh domiflist ${new_img} | awk '/network/ {print $5}') &&
    echo ${mac} &&
    ip=$(/usr/sbin/arp -an  |grep "${mac}" | grep -o -P '(?<=\? \().*(?=\) .*)') &&
    echo ${ip}
    """, returnStdout: true)
}   

node() {
    def LIBVIRT_SOCKET = "--connect=qemu:///system"
    if (DISTRO_RELEASE == xenial){
        old_img = 'devstack-generic-ubuntu-xenial'
    }
    else {
        'Distro release not present'
    }

    def SSHUSER = root
    def SSHPASS = r00tme
    
    stage ('Create VM') {

    try {
        ENV_IP = CreateVM("${params.ENV_NAME}","${LIBVIRT_SOCKET}","${params.DISTRO_RELEASE}").trim()
        echo "${ENV_IP}"
    }
    }
    
    stage ('Deploy Devstack') {
      writeFile file: '/tmp/ssh-config', text: """\
        StrictHostKeyChecking no
        UserKnownHostsFile /dev/null
        ForwardAgent yes
        Port 22
    """.stripIndent()

      writeFile file: '/tmp/local.conf', text: "${LOCAL_CONF}"

      writeFile file '/tmp/sckrips.sh', text: """\
        #!/bin/bash -x
	    set -e
	    useradd -s /bin/bash -d /opt/stack -m stack
	    sed -i "s/devstack-generic/$ENV_NAME/g" /etc/hosts
	    hostname $ENV_NAME
	    echo "stack:r00tme" |  chpasswd
	    git clone $DEVSTACK_REPO -b $DEVSTACK_BRANCH /opt/stack/devstack
	    chown -R stack:stack /opt/stack/devstack
      """.stripIndent()

      sh "chmod +x /tmp/sckrips.sh"

      withEnv(["SSHPASS=${SSHPASS}"]) {

        sh "sshpass  -e scp -qF /tmp/ssh-config /tmp/sckrips.sh root@${ENV_IP}:."
        sh "sshpass -e ssh -F /tmp/ssh-config root@${ENV_IP} ./sckrips.sh"
        sh "sshpass -e scp -qF /tmp/ssh-config /tmp/local.conf stack@${ENV_IP}:/opt/stack/devstack"
        sh "sshpass -e ssh -F /tmp/ssh-config stack@${ENV_IP} cd devstack; ./stack.sh; exit"
      }
   }
}
